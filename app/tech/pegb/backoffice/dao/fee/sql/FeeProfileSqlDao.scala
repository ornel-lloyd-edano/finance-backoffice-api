package tech.pegb.backoffice.dao.fee.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm.SqlParser.scalar
import anorm._
import cats.implicits._
import cats.syntax.either._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.Dao.{EntityId, IntEntityId, UUIDEntityId}
import tech.pegb.backoffice.dao.SqlDao.genId
import tech.pegb.backoffice.dao.fee.dto._
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.dao.provider.abstraction.ProviderDao
import tech.pegb.backoffice.dao.provider.dto.ProviderCriteria
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.{Dao, DaoError, SqlDao, fee}
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Utils}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class FeeProfileSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig,
    historySqlDao: FeeProfileHistorySqlDao,
    providerDao: ProviderDao,
    kafkaDBSyncService: KafkaDBSyncService) extends fee.abstraction.FeeProfileDao with MostRecentUpdatedAtGetter[FeeProfile, FeeProfileCriteria] with SqlDao {

  import FeeProfileSqlDao._

  protected def getUpdatedAtColumn: String = s"${FeeProfileSqlDao.TableAlias}.${FeeProfileSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = FeeProfileSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ FeeProfile = (row: Row) ⇒ convertRowToEntity(row)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[FeeProfileCriteria]): String = generateWhere(criteriaDto)

  def insertFeeProfile(dto: FeeProfileToInsert): DaoResponse[FeeProfile] = {
    withTransaction({ implicit cxn ⇒

      val createdFeeProfile: DaoResponse[FeeProfile] = for {
        providerId ← dto.provider match {
          case Some(providerName) ⇒
            providerDao.getByCriteria(ProviderCriteria(providerName), None, None, None)(Some(cxn)).map(_.headOption.map(_.id)).flatMap(
              {
                case Some(provider) ⇒
                  Right(Some(provider))
                case None ⇒
                  Left(DaoError.EntityNotFoundError(s"Failed to insert fee profile because because provider [${providerName}] was not found."))
              })
          case None ⇒ Right(None)
        }

        generatedId ← Try {
          insertFeeProfileQuery
            .on(
              cFeeType → dto.feeType,
              cUserType → dto.userType,
              cTier → dto.tier,
              cSubscriptionType → dto.subscriptionType,
              cTransactionType → dto.transactionType,
              cChannel → dto.channel,
              cProviderId → providerId,
              cInstrument → dto.instrument,
              cCalculationMethod → dto.calculationMethod,
              cMaxFee → dto.maxFee,
              cMinFee → dto.minFee,
              cFeeAmount → dto.feeAmount,
              cFeeRatio → dto.feeRatio,
              cFeeMethod → dto.feeMethod,
              cTaxIncluded → dto.taxIncluded,
              cUuid → genId(),
              cCreatedBy → dto.createdBy,
              cCreatedAt → dto.createdAt,
              cUpdatedAt → dto.createdAt, //not nullable in db and same as created at on insertion
              cUpdatedBy → dto.createdBy, //not nullable in db and same as created by on insertion
              cCurrencyId → dto.currencyId)
            .executeInsert(scalar[Int].single)
        }.toEither.left.map(_ ⇒ genericDbError("Failed to insert fee profile"))

        feeProfile ← internalGetFeeProfile(generatedId.asEntityId)
          .toRight(genericDbError("Failed to fetch created profile"))
        ranges ← {
          implicit val optionCnx = Option(cxn)
          dto.ranges match {
            case Some(rangeSeq) ⇒
              insertFeeProfileRange(generatedId.asEntityId, rangeSeq.map(_.copy(feeProfileId = Option(generatedId)))).map(Option(_))
            case None ⇒
              Right(None)
          }
        }

        _ ← Try(kafkaDBSyncService.sendInsert(TableName, feeProfile.copy(ranges = ranges))).toEither.fold(error ⇒ {
          logger.warn(s"Failed to produce created fee profile [${dto.toSmartString}] to kafka", error)
          Right(())
        }, _ ⇒ Right(()))

      } yield {
        feeProfile.copy(ranges = ranges)
      }
      createdFeeProfile
    })
  }

  def insertFeeProfileRange(feeProfileId: EntityId, dtoSeq: Seq[FeeProfileRangeToInsert])(implicit connectionOption: Option[Connection] = None): DaoResponse[Seq[FeeProfileRange]] = {
    withTransaction({ implicit cxn ⇒
      Try {
        assert(dtoSeq.nonEmpty, "FeeProfileRangeToInsert should not be empty")
        val rowsAsNamedParameterSeq = dtoSeq.map { dto ⇒
          Seq(
            NamedParameter(cFeeProfileId, dto.feeProfileId),
            NamedParameter(cMax, dto.max),
            NamedParameter(cMin, dto.min),
            NamedParameter(cFeeAmount, dto.feeAmount),
            NamedParameter(cFeeRatio, dto.feeRatio))
        }

        val dbConnection = connectionOption.getOrElse(cxn)

        batchInsertRangeQuery(rowsAsNamedParameterSeq.head, rowsAsNamedParameterSeq.tail: _*).execute()(dbConnection)
        val ranges = internalGetFeeProfileRanges(feeProfileId)(dbConnection)

        ranges.foreach(range ⇒ kafkaDBSyncService.sendInsert(FeeProfileRangesTable, range))
        ranges
      }.toEither.leftMap(t ⇒ {
        logger.error("error encountered in [insertFeeProfileRange]", t)
        genericDbError(s"Failed to fetch created profile ranges")
      })
    })
  }

  def getFeeProfile(id: Dao.EntityId): DaoResponse[Option[FeeProfile]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetFeeProfile(id)
    }, s"Failed to fetch fee profile $id")
  }

  def getFeeProfileByCriteria(
    criteria: FeeProfileCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[FeeProfile]] = withConnection({ implicit connection ⇒
    val whereFilter = generateWhere(Some(criteria))

    val feeProfilesByCriteriaSql = findFeeProfilesByCriteriaQuery(whereFilter, ordering, limit, offset)

    feeProfilesByCriteriaSql.as(feeProfileRowParser.*)
  }, s"Error while retrieving feeProfiles by criteria:$criteria")

  def getFeeProfileRangesByFeeProfileId(feeProfileId: EntityId): DaoResponse[Seq[FeeProfileRange]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetFeeProfileRanges(feeProfileId)
    }, s"Failed to fetch ranges for fee profile $feeProfileId")
  }

  def countFeeProfileByCriteria(criteria: FeeProfileCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateWhere(Some(criteria))
    val countByCriteriaSql = countFeeProfilesByCriteriaQuery(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def updateFeeProfile(id: Dao.EntityId, dto: FeeProfileToUpdate): DaoResponse[Option[FeeProfile]] = {
    withTransaction({ implicit cxn: Connection ⇒
      for {
        existing ← internalGetFeeProfile(id)
        updateResult = updateQuery(id, dto).executeUpdate()
        _ = dto.ranges.map(ranges ⇒ if (ranges == null) clearRangesForFee(existing.id).execute()
        else recreateFeeProfileRanges(existing.id, ranges))
        updated ← if (updateResult.isUpdated) {
          internalGetFeeProfile(id, dto.deletedAt.isEmpty)
        } else {
          throw new IllegalStateException(s"Update failed. Fee profile $id has been modified by another process.")
        }
        _ = historySqlDao.internalCreateHistoryRecord(existing, updated, dto.updatedBy, dto.updatedAt, None)
      } yield {
        kafkaDBSyncService.sendUpdate(TableName, updated)
        updated
      }
    }, s"Failed to update fee profile $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update FeeProfile ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })

  }

  def updateFeeProfileRange(rangeId: Int, dto: FeeProfileRangeToUpdate) = ???

  def deleteFeeProfileRange(rangeId: Int)(implicit requestId: UUID): DaoResponse[Option[FeeProfileRange]] = ???

  private[sql] def internalGetFeeProfile(
    entityId: EntityId,
    activeOnly: Boolean = true)(
    implicit
    cxn: Connection): Option[FeeProfile] = {
    val query = entityId match {
      case UUIDEntityId(uuid) ⇒
        fetchByUuidQuery(activeOnly).on(cUuid → uuid)
      case IntEntityId(id) ⇒
        fetchByIdQuery(activeOnly).on(cId → id)
    }
    val ranges = internalGetFeeProfileRanges(entityId)
    query.executeQuery().as(feeProfileRowParser.singleOpt)
      .map { feeProfile ⇒
        if (feeProfile.calculationMethod.toLowerCase.contains("staircase"))
          feeProfile.copy(ranges = Some(ranges))
        else
          feeProfile
      }
  }

  private[sql] def internalGetFeeProfileRanges(entityId: EntityId)(implicit cxn: Connection): Seq[FeeProfileRange] = {
    val query = entityId match {
      case UUIDEntityId(uuid) ⇒
        fetchRangesByFeeProfileUuidQuery.on(cUuid → uuid)
      case IntEntityId(id) ⇒
        fetchRangesByFeeProfileIdQuery.on(cId → id)
    }
    query.executeQuery().as(feeProfileRangesRowParser.*)
  }

  private def recreateFeeProfileRanges(
    feeProfileId: Int,
    ranges: Seq[FeeProfileRangeToInsert])(
    implicit
    cxn: Connection): Unit = {
    clearRangesForFee(feeProfileId).execute()
    val createdAt = Utils.nowAsLocal()
    ranges.foreach { range ⇒
      createRangeForFee(feeProfileId, range, createdAt)
        .executeInsert(scalar[Int].singleOpt)
    }
  }
}

object FeeProfileSqlDao {

  val TableName = "fee_profiles"
  val TableAlias = "fp"
  val FeeProfileRangesTable = "fee_profile_ranges"
  val FeeProfileRangesAlias = "fpr"
  val CurrencyTable = "currencies"
  val CurrencyAlias = "c"

  //fee_profiles columns
  val cId = "id"
  val cFeeType = "fee_type"
  val cUserType = "user_type"
  val cSubscriptionType = "subscription_type"
  val cTransactionType = "transaction_type"
  val cChannel = "channel"
  val cTier = "tier"

  val cProviderId = "provider_id"
  val cInstrument = "instrument"
  val cCalculationMethod = "calculation_method"
  val cMaxFee = "max_fee"
  val cMinFee = "min_fee"
  val cFeeAmount = "fee_amount"
  val cFeeRatio = "fee_ratio"
  val cFeeMethod = "fee_method"
  val cTaxIncluded = "tax_included"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
  val cUuid = "uuid"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
  val cCurrencyId = "currency_id"
  val cDeletedAt = "deleted_at"

  //fee_profile_ranges columns
  val cFeeProfileId = "fee_profile_id"
  val cMax = "max"
  val cMin = "min"

  //currencies columns
  val cCurrencyCode = "currency_code"
  val cIsCurrencyActive = "is_active"
  val cCurrencyName = "currency_name"

  private val insertFeeProfileQuery =
    SQL(s"INSERT INTO $TableName ($cFeeType, $cUserType, $cTier, $cSubscriptionType, $cTransactionType, $cChannel, " +
      s"$cProviderId, $cInstrument, $cCalculationMethod, $cMaxFee, $cMinFee, $cFeeAmount, $cFeeRatio, $cFeeMethod, " +
      s"$cTaxIncluded, $cUuid, $cCreatedBy, $cCreatedAt, $cUpdatedBy, $cUpdatedAt, $cCurrencyId) " +
      s"VALUES ({$cFeeType}, {$cUserType}, {$cTier}, {$cSubscriptionType}, {$cTransactionType}, {$cChannel}, " +
      s"{$cProviderId}, {$cInstrument}, {$cCalculationMethod}, {$cMaxFee}, {$cMinFee}, {$cFeeAmount}, {$cFeeRatio}, {$cFeeMethod}, " +
      s"{$cTaxIncluded}, {$cUuid}, {$cCreatedBy}, {$cCreatedAt}, {$cUpdatedBy}, {$cUpdatedAt} , {$cCurrencyId})")

  private val insertRangesQuery =
    s"INSERT INTO $FeeProfileRangesTable ($cFeeProfileId, $cMax, $cMin, $cFeeAmount, $cFeeRatio, $cCreatedAt, $cUpdatedAt) " +
      s"VALUES ({$cFeeProfileId}, {$cMax}, {$cMin}, {$cFeeAmount}, {$cFeeRatio}, now(), now())"

  private val qCommonJoin =
    s"""
       |FROM $TableName $TableAlias
       |JOIN $CurrencyTable $CurrencyAlias
       |ON $TableAlias.$cCurrencyId = $CurrencyAlias.$cId
       |
       |LEFT JOIN ${ProviderSqlDao.TableName}
       |ON $TableAlias.$cProviderId = ${ProviderSqlDao.TableName}.${Provider.cId}
     """.stripMargin
  private val qCommonSelect = s"SELECT $TableAlias.*, $CurrencyAlias.$cCurrencyName as $cCurrencyCode, ${ProviderSqlDao.TableName}.${Provider.cName} $qCommonJoin"

  private def batchInsertRangeQuery(firstRow: Seq[NamedParameter], tail: Seq[NamedParameter]*) = {
    BatchSql(insertRangesQuery, firstRow, tail: _*)
  }

  private def baseFindFeeProfilesByCriteria(filters: String): String =
    s"$qCommonSelect $filters".stripMargin

  private def countFeeProfilesByCriteriaQuery(filters: String): SqlQuery = {
    SQL(s"SELECT COUNT(*) as n $qCommonJoin $filters")
  }

  private def findFeeProfilesByCriteriaQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(o ⇒ o.copy(underlying = o.underlying.map({
      case ord if ord.field === "other_party" ⇒ ord.copy(field = "providers.name")
      case ord ⇒ ord
    })).toString)
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    SQL(s"""${baseFindFeeProfilesByCriteria(filters)} $ordering $pagination""".stripMargin)
  }

  private def fetchByIdQuery(isActiveOnly: Boolean): SqlQuery = {
    val filters = if (isActiveOnly) {
      s"""WHERE $TableAlias.$cId = {$cId} AND $TableAlias.$cDeletedAt IS NULL"""
    } else {
      s"""WHERE $TableAlias.$cId = {$cId}"""
    }
    SQL(s"""${baseFindFeeProfilesByCriteria(filters)}""".stripMargin)
  }

  private def fetchByUuidQuery(isActiveOnly: Boolean): SqlQuery = {
    val filters = if (isActiveOnly) {
      s"WHERE $TableAlias.$cUuid = {$cUuid} AND $TableAlias.$cDeletedAt IS NULL"
    } else {
      s"WHERE $TableAlias.$cUuid = {$cUuid}"
    }
    SQL(s"""${baseFindFeeProfilesByCriteria(filters)}""".stripMargin)
  }

  private def updateQuery(id: Dao.EntityId, dto: FeeProfileToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = convertEntityIdToParam(id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def clearRangesForFee(feeProfileId: Int): SimpleSql[Row] = {
    SQL(s"DELETE FROM $FeeProfileRangesTable WHERE $cFeeProfileId = {$cFeeProfileId}")
      .on(cFeeProfileId → feeProfileId)
  }

  private def createRangeForFee(
    feeId: Int,
    range: FeeProfileRangeToInsert,
    createdAt: LocalDateTime): SimpleSql[Row] = {
    SQL(s"INSERT INTO $FeeProfileRangesTable ($cFeeProfileId, $cMin, $cMax, $cFeeAmount, $cFeeRatio, $cCreatedAt, $cUpdatedAt) " +
      s"VALUES ({$cFeeProfileId}, {$cMin}, {$cMax}, {$cFeeAmount}, {$cFeeRatio}, {$cCreatedAt}, {$cUpdatedAt})")
      .on(cFeeProfileId → feeId, cMin → range.min, cMax → range.max, cFeeAmount → range.feeAmount, cFeeRatio → range.feeRatio, cCreatedAt → createdAt, cUpdatedAt → createdAt)
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def convertEntityIdToParam(id: Dao.EntityId): NamedParameter = id match {
    case UUIDEntityId(uuid) ⇒ (cUuid, uuid)
    case IntEntityId(intId) ⇒ (cId, intId)
  }

  def convertRowToEntity(row: Row): FeeProfile = {
    FeeProfile(
      id = row[Int](cId),
      uuid = row[UUID](cUuid),
      feeType = row[String](cFeeType),
      userType = row[String](cUserType),
      tier = row[String](cTier),
      subscription = row[String](cSubscriptionType),
      transactionType = row[String](cTransactionType),
      channel = row[Option[String]](cChannel),
      provider = row[Option[String]](s"${ProviderSqlDao.TableName}.${Provider.cName}"),
      instrument = row[Option[String]](cInstrument),
      calculationMethod = row[String](cCalculationMethod),
      maxFee = row[Option[BigDecimal]](cMaxFee),
      minFee = row[Option[BigDecimal]](cMinFee),
      feeAmount = row[Option[BigDecimal]](cFeeAmount),
      feeRatio = row[Option[BigDecimal]](cFeeRatio),
      feeMethod = row[String](cFeeMethod),
      taxIncluded = row[String](cTaxIncluded),
      ranges = None,
      currencyCode = row[String](cCurrencyCode),
      createdAt = row[LocalDateTime](cCreatedAt),
      createdBy = row[String](cCreatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      deletedAt = row[Option[LocalDateTime]](cDeletedAt))
  }

  private val feeProfileRowParser: RowParser[FeeProfile] = row ⇒ {
    Try {
      convertRowToEntity(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val feeProfileRangesRowParser: RowParser[FeeProfileRange] = row ⇒ {
    Try {
      FeeProfileRange(
        id = row[Int](cId),
        feeProfileId = row[Option[Int]](cFeeProfileId),
        max = row[Option[BigDecimal]](cMax),
        min = row[Option[BigDecimal]](cMin),
        feeAmount = row[Option[BigDecimal]](cFeeAmount),
        feeRatio = row[Option[BigDecimal]](cFeeRatio))

    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def generateWhere(mayBeCriteria: Option[FeeProfileCriteria]): String = {
    import SqlDao._

    mayBeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql(none, TableAlias.some)),
        criteria.provider.map(_.toSql(Some(Provider.cName), Some(ProviderSqlDao.TableName))),
        criteria.feeType.map(_.toSql(none, TableAlias.some)),
        criteria.userType.map(_.toSql(none, TableAlias.some)),
        criteria.tier.map(_.toSql(none, TableAlias.some)),
        criteria.subscriptionType.map(_.toSql(none, TableAlias.some)),
        criteria.transactionType.map(_.toSql(none, TableAlias.some)),
        criteria.channel.map(_.toSql(none, TableAlias.some)),
        criteria.instrument.map(_.toSql(none, TableAlias.some)),
        criteria.calculationMethod.map(_.toSql(none, TableAlias.some)),
        criteria.feeMethod.map(_.toSql(none, TableAlias.some)),
        criteria.taxIncluded.map(_.toSql(none, TableAlias.some)),
        criteria.currencyCode.map(_.toSql(none, CurrencyAlias.some)),
        criteria.isDeleted.map(d ⇒ toNullSql(cDeletedAt, !d.value, TableAlias.some)),
        CriteriaField(cIsCurrencyActive, 1).toSql(none, Some(CurrencyAlias)).some).flatten.toSql

    }.getOrElse("")
  }

  private def baseFindFeeProfileRanges(filters: String): String = {
    s"""SELECT $FeeProfileRangesAlias.*
       |FROM $FeeProfileRangesTable $FeeProfileRangesAlias
       |JOIN $TableName $TableAlias
       |ON $FeeProfileRangesAlias.$cFeeProfileId = $TableAlias.$cId
       |$filters
       |ORDER BY $FeeProfileRangesAlias.$cMin""".stripMargin
  }

  private def fetchRangesByFeeProfileIdQuery: SqlQuery = {
    val filters = s"WHERE $TableAlias.$cId = {$cId}"
    SQL(s"""${baseFindFeeProfileRanges(filters)}""".stripMargin)
  }

  private def fetchRangesByFeeProfileUuidQuery: SqlQuery = {
    val filters = s"WHERE $TableAlias.$cUuid = {$cUuid}"
    SQL(s"""${baseFindFeeProfileRanges(filters)}""".stripMargin)
  }

}
