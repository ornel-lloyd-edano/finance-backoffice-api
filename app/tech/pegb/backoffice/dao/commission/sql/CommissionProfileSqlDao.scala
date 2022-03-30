package tech.pegb.backoffice.dao.commission.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.SqlParser.scalar
import anorm.{BatchSql, NamedParameter, Row, RowParser, SQL, SimpleSql, SqlRequestError}
import cats.data.NonEmptyList
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.commission.abstraction.CommissionProfileDao
import tech.pegb.backoffice.dao.commission.dto.{CommissionProfileCriteria, CommissionProfileRangeToInsert, CommissionProfileToInsert, CommissionProfileToUpdate}
import tech.pegb.backoffice.dao.commission.entity.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.provider.abstraction.ProviderDao
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.dao.util.Implicits._

import scala.util.Try

class CommissionProfileSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig,
    providerDao: ProviderDao,
    kafkaDBSyncService: KafkaDBSyncService) extends CommissionProfileDao with MostRecentUpdatedAtGetter[CommissionProfile, CommissionProfileCriteria] with SqlDao {

  import CommissionProfileSqlDao._

  protected def getUpdatedAtColumn: String = s"${CommissionProfile.TableAlias}.${CommissionProfile.cUpdatedAt}"

  protected def getMainSelectQuery: String = selectCommissionProfileString(s"${CommissionProfile.TableAlias}.*, ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} as ${CommissionProfile.cCurrencyCode}")

  protected def getRowToEntityParser: Row ⇒ CommissionProfile = (row: Row) ⇒ convertRowToCommissionProfileEntity(row)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[CommissionProfileCriteria]): String = generateCommissionProfileWhereFilter(criteriaDto)

  def insertCommissionProfile(dto: CommissionProfileToInsert): DaoResponse[CommissionProfile] = {
    import CommissionProfile._

    withTransactionAndFlatten({ implicit cxn ⇒

      val commissionProfile: DaoResponse[CommissionProfile] = (for {
        commissionProfileId ← Either.fromTry(Try {
          insertCommissionProfileSql.on(
            cUuid → dto.uuid,
            cBusinessType → dto.businessType,
            cTier → dto.tier,
            cSubscriptionType → dto.subscriptionType,
            cTransactionType → dto.transactionType,
            cCurrencyId → dto.currencyId,
            cChannel → dto.channel,
            cInstrument → dto.instrument,
            cCalculationMethod → dto.calculationMethod,
            cMaxCommission → dto.maxCommission,
            cMinCommission → dto.minCommission,
            cCommissionAmount → dto.commissionAmount,
            cCommissionRatio → dto.commissionRatio,
            cCreatedBy → dto.createdBy,
            cUpdatedBy → dto.createdBy,
            cCreatedAt → dto.createdAt,
            cUpdatedAt → dto.createdAt).executeInsert(scalar[Int].single)
        }).leftMap { err ⇒
          logger.error("[insertCommissionProfile] insert commission profile sql error", err)
          genericDbError("Failed to insert fee profile")
        }

        createdProfile ← Either.fromOption(
          internalGetCommissionProfileByUUID(dto.uuid),
          genericDbError("Failed to fetch created profile"))

        ranges ← dto.ranges match {
          case None ⇒ none[Seq[CommissionProfileRange]].asRight[DaoError]
          case Some(rangeDto) ⇒ insertCommissionProfileRange(commissionProfileId, rangeDto, dto.createdAt)(cxn.some).map(_.some)
        }

        finalCommissionProfile = createdProfile //.addRanges(ranges)

        _ ← Try(kafkaDBSyncService.sendInsert(TableName, finalCommissionProfile)).toEither.fold(error ⇒ {
          logger.warn(s"Failed to produce created commission profile [${dto.toSmartString}] to kafka", error)
          Right(())
        }, _ ⇒ Right(()))

      } yield {
        finalCommissionProfile
      })

      commissionProfile
    }, s"Error encountered on insert of CommissionProfile")
  }

  def getCommissionProfileByCriteria(
    criteria: CommissionProfileCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[CommissionProfile]] = withConnection({ implicit connection ⇒

    import CommissionProfile._

    val whereFilter = generateCommissionProfileWhereFilter(criteria.some)

    val order = ordering.fold(s"ORDER BY $TableAlias.$cCreatedAt DESC")(_.toString)
    val pagination = SqlDao.getPagination(limit, offset)

    val columns = s"""
                     |$TableAlias.*,
                     |${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} as $cCurrencyCode
       """.stripMargin

    val sqlQuery = s"""
                      |${selectCommissionProfileString(columns)}
                      |$whereFilter
                      |$order
                      |$pagination""".stripMargin

    logger.info(s"[getCommissionProfileByCriteria] $sqlQuery")

    SQL(sqlQuery).as(commissionRowParser.*)

  }, s"Error while retrieving CommissionProfile by criteria: $criteria")

  def getCommissionProfileRangeByCommissionId(commissionId: Int): DaoResponse[Seq[CommissionProfileRange]] = withConnection({ implicit connection ⇒

    internalGetRangesByCommissionId(commissionId)

  }, s"Error while retrieving Commission Profile Range by CommissionId: $commissionId")

  def countCommissionProfileByCriteria(criteria: CommissionProfileCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒

    val whereFilter = generateCommissionProfileWhereFilter(criteria.toOption)

    val columns = "COUNT(*) as n"

    val sqlQuery = s"""
                      |${selectCommissionProfileString(columns)}
                      |$whereFilter""".stripMargin

    logger.info(s"[countCommissionProfileByCriteria] $sqlQuery")

    val countQuery = SQL(sqlQuery)

    countQuery.as(countQuery.defaultParser.singleOpt)
      .map(convertRowToCount(_))
      .getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def updateCommissionProfile(uuid: String, dto: CommissionProfileToUpdate): DaoResponse[Option[CommissionProfile]] = {
    /*withTransaction({ implicit cxn: Connection ⇒
      for {
        existing ← internalGetCommissionProfileByUUID(uuid)
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
      })*/
    ???
  }

  private def insertCommissionProfileRange(
    commissionProfileId: Int,
    dtoSeq: NonEmptyList[CommissionProfileRangeToInsert],
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection] = None): DaoResponse[Seq[CommissionProfileRange]] = {
    import CommissionProfileRange._

    withTransaction({ cxn: Connection ⇒
      implicit val connection = maybeConnection.getOrElse(cxn)

      val commissionRanges = dtoSeq.map { dto ⇒
        Seq(
          NamedParameter(cCommissionProfileId, commissionProfileId),
          NamedParameter(cMin, dto.min),
          NamedParameter(cMax, dto.max),
          NamedParameter(cCommissionAmount, dto.commissionAmount),
          NamedParameter(cCommissionRatio, dto.commissionRatio),
          NamedParameter(cCreatedAt, createdAt),
          NamedParameter(cUpdatedAt, createdAt))
      }

      BatchSql(insertCommissionProfileRangesString, commissionRanges.head, commissionRanges.tail: _*).execute()

      internalGetRangesByCommissionId(commissionProfileId)

    }, s"Error encountered on bulk insert of CommissionProfileRange",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not create CommissionProfileRange, SQLException encountered"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error(s"[insertCommissionProfileRange] generic error", generic)
          genericDbError(s"error encountered on bulk insert of CommissionProfileRange")
      })
  }

  private def internalGetCommissionProfileByUUID(uuid: String)(implicit cxn: Connection): Option[CommissionProfile] = {
    import CommissionProfile._

    val fields =
      s"""
         |$TableAlias.*,
         |${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} as $cCurrencyCode
       """.stripMargin

    SQL(s"""
        |${selectCommissionProfileString(fields)}
        |WHERE $TableAlias.$cUuid = {$cUuid}""".stripMargin)
      .on(cUuid → uuid)
      .executeQuery()
      .as(commissionRowParser.singleOpt)
  }

  private def internalGetRangesByCommissionId(commissionId: Int)(implicit cxn: Connection): Seq[CommissionProfileRange] = {
    import CommissionProfileRange._

    SQL(s"SELECT * FROM $TableName where $cCommissionProfileId = {$cCommissionProfileId} ORDER BY id")
      .on(cCommissionProfileId → commissionId)
      .executeQuery()
      .as(commissionRangeRowParser.*)
  }
}

object CommissionProfileSqlDao {

  def selectCommissionProfileString(fields: String) = {
    import CommissionProfile._
    s"""
         |SELECT $fields
         |FROM $TableName $TableAlias
         |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
         |ON $TableAlias.$cCurrencyId = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
      """.stripMargin
  }

  private def generateCommissionProfileWhereFilter(mayBeCriteria: Option[CommissionProfileCriteria]): String = {
    import SqlDao._
    import CommissionProfile._

    mayBeCriteria.map { criteria ⇒

      Seq(
        criteria.uuid.map(_.toSql(cUuid.some, TableAlias.some)),
        criteria.businessType.map(_.toSql(cBusinessType.some, TableAlias.some)),
        criteria.tier.map(_.toSql(cTier.some, TableAlias.some)),
        criteria.subscriptionType.map(_.toSql(cSubscriptionType.some, TableAlias.some)),
        criteria.transactionType.map(_.toSql(cTransactionType.some, TableAlias.some)),
        criteria.currency.map(_.toSql(CurrencySqlDao.cName.some, CurrencySqlDao.TableAlias.some)),
        criteria.channel.map(_.toSql(cChannel.some, TableAlias.some)),
        criteria.instrument.map(_.toSql(cInstrument.some, TableAlias.some)),
        criteria.calculationMethod.map(_.toSql(cCalculationMethod.some, TableAlias.some)),
        criteria.isDeleted.map(d ⇒ toNullSql(cDeletedAt, !d.value, TableAlias.toOption))).flatten.toSql
    }.getOrElse("")
  }

  val insertCommissionProfileString = {
    import CommissionProfile._

    val TableFields = Seq(cUuid, cBusinessType, cTier, cSubscriptionType, cTransactionType, cCurrencyId, cChannel, cInstrument,
      cCalculationMethod, cMaxCommission, cMinCommission, cCommissionAmount, cCommissionRatio,
      cCreatedBy, cUpdatedBy, cCreatedAt, cUpdatedAt)

    val TableFieldStr = TableFields.mkString(",")
    val ValuesPlaceHolders = TableFields.map(c ⇒ s"{$c}").mkString(",")

    s"INSERT INTO $TableName ($TableFieldStr) VALUES ($ValuesPlaceHolders)"
  }

  val insertCommissionProfileRangesString = {
    import CommissionProfileRange._

    val TableFields = Seq(cCommissionProfileId, cMin, cMax, cCommissionAmount, cCommissionRatio, cCreatedAt, cUpdatedAt)

    val TableFieldStr = TableFields.mkString(",")
    val ValuesPlaceHolders = TableFields.map(c ⇒ s"{$c}").mkString(",")

    s"INSERT INTO $TableName ($TableFieldStr) VALUES ($ValuesPlaceHolders)"
  }

  val insertCommissionProfileSql = SQL(insertCommissionProfileString)
  val insertCommissionProfileRangesSql = SQL(insertCommissionProfileRangesString)

  def convertRowToCommissionProfileEntity(row: Row): CommissionProfile = {
    import CommissionProfile._

    CommissionProfile(
      id = row[Int](cId),
      uuid = row[String](cUuid),
      businessType = row[String](cBusinessType),
      tier = row[String](cTier),
      subscriptionType = row[String](cSubscriptionType),
      transactionType = row[String](cTransactionType),
      currencyId = row[Int](cCurrencyId),
      currencyCode = row[String](cCurrencyCode),
      channel = row[Option[String]](cChannel),
      instrument = row[Option[String]](cInstrument),
      calculationMethod = row[String](cCalculationMethod),
      maxCommission = row[Option[BigDecimal]](cMaxCommission),
      minCommission = row[Option[BigDecimal]](cMinCommission),
      commissionAmount = row[Option[BigDecimal]](cCommissionAmount),
      commissionRatio = row[Option[BigDecimal]](cCommissionRatio),
      ranges = None,
      createdBy = row[String](cCreatedBy),
      updatedBy = row[String](cUpdatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedAt = row[LocalDateTime](cUpdatedAt),
      deletedAt = row[Option[LocalDateTime]](cDeletedAt))
  }

  def convertRowToCommissionProfileRangeEntity(row: Row): CommissionProfileRange = {
    import CommissionProfileRange._

    CommissionProfileRange(
      id = row[Int](cId),
      commissionProfileId = row[Int](cCommissionProfileId),
      min = row[BigDecimal](cMin),
      max = row[Option[BigDecimal]](cMax),
      commissionAmount = row[Option[BigDecimal]](cCommissionAmount),
      commissionRatio = row[Option[BigDecimal]](cCommissionRatio),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedAt = row[LocalDateTime](cUpdatedAt))
  }

  private val commissionRowParser: RowParser[CommissionProfile] = row ⇒ {
    Try {
      convertRowToCommissionProfileEntity(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val commissionRangeRowParser: RowParser[CommissionProfileRange] = row ⇒ {
    Try {
      convertRowToCommissionProfileRangeEntity(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def updateQuery(id: Int, dto: CommissionProfileToUpdate): SimpleSql[Row] = {
    import CommissionProfile._

    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }
}
