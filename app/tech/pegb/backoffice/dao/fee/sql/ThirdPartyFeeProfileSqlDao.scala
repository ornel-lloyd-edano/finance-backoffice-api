package tech.pegb.backoffice.dao.fee.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.scalar
import anorm.{BatchSql, NamedParameter, RowParser, SQL, SqlRequestError, _}
import cats.syntax.either._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.fee.dto._
import tech.pegb.backoffice.dao.fee.entity.{ThirdPartyFeeProfile, ThirdPartyFeeProfileRange}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.dao.provider.abstraction.ProviderDao
import tech.pegb.backoffice.dao.provider.dto.ProviderCriteria
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.{DaoError, SqlDao, fee}
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class ThirdPartyFeeProfileSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig,
    providerDao: ProviderDao,
    kafkaDBSyncService: KafkaDBSyncService) extends fee.abstraction.ThirdPartyFeeProfileDao with SqlDao {

  import ThirdPartyFeeProfileSqlDao._

  def createThirdPartyFeeProfile(dto: ThirdPartyFeeProfileToInsert): DaoResponse[ThirdPartyFeeProfile] = {
    withTransaction({ implicit conn ⇒

      for {
        providerId ← providerDao.getByCriteria(ProviderCriteria(dto.provider), None, None, None)(Some(conn)).map(_.headOption.map(_.id)).fold(
          err ⇒ Left(DaoError.GenericDbError(s"Failed to insert third-party fee profile when fetching provider [${dto.provider}]. Reason: ${err.message}")),
          {
            case Some(provider) ⇒
              Right(provider)
            case _ ⇒
              Left(DaoError.EntityNotFoundError(s"Failed to insert third-party fee profile because because provider [${dto.provider}] was not found."))
          })

        id ← Try {
          insertProfileQuery.on(getNamedParameter(dto, providerId): _*).executeInsert(scalar[Int].single)
        }.toEither.left.map(err ⇒ DaoError.GenericDbError(s"Insert of third-party fee profile failed. Reason: ${err.getMessage}"))

        profile ← internalGetThirdPartyFeeProfile(id.toString).toRight(genericDbError("Failed to fetch created third party fee profile"))
        ranges ← {
          implicit val optionCnx: Option[Connection] = Option(conn)
          dto.ranges match {
            case Some(rangeSeq) ⇒
              createThirdPartyFeeProfileRange(id.toString, rangeSeq).map(Option(_))
            case None ⇒ Right(None)
          }
        }

        _ ← Try(kafkaDBSyncService.sendInsert(TableName, profile.copy(ranges = ranges))).toEither.fold(error ⇒ {
          logger.warn(s"Failed to produce created third-party fee profile [${dto.toSmartString}] to kafka", error)
          Right(())
        }, _ ⇒ Right(()))
      } yield {
        profile.copy(ranges = ranges)
      }
    })

  }

  def createThirdPartyFeeProfileRange(
    thirdPartyFeeProfileId: String,
    dto: Seq[ThirdPartyFeeProfileRangeToInsert])(implicit connectionOption: Option[Connection] = None): DaoResponse[Seq[ThirdPartyFeeProfileRange]] =
    withTransaction({ implicit cxn ⇒
      Try {
        assert(dto.nonEmpty, "ThirdPartyFeeProfileRangeToInsert should not be empty")

        val rowsAsNamedParameterSeq = dto.map(dto ⇒ getNamedParameterForRange(thirdPartyFeeProfileId, dto))

        val dbConnection = connectionOption.getOrElse(cxn)

        batchInsertRangeQuery(rowsAsNamedParameterSeq.head, rowsAsNamedParameterSeq.tail: _*).execute()(dbConnection)

        val ranges = internalGetThirdPartyFeeRange(thirdPartyFeeProfileId)(dbConnection)
        ranges.foreach(range ⇒ kafkaDBSyncService.sendInsert(ThirdPartyFeeProfileRangesTable, range))

        ranges
      }.toEither.leftMap(t ⇒ {
        logger.error("error encountered in [createThirdPartyFeeProfileRange]", t)
        genericDbError(s"Failed to fetch created third party fee profile ranges")
      })
    })

  def getThirdPartyFeeProfile(id: String): DaoResponse[Option[ThirdPartyFeeProfile]] =
    withConnection({ implicit cxn: Connection ⇒
      internalGetThirdPartyFeeProfile(id)
    }, s"Failed to fetch third party fee profile $id")

  def getThirdPartyFeeProfileByCriteria(criteria: ThirdPartyFeeProfileCriteria, ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[ThirdPartyFeeProfile]] =
    withConnection({ implicit connection ⇒

      val whereClause = where(Some(criteria))

      val getThirdPartyFeeProfiles = findThirdPartyFeeProfilesByCriteriaQuery(whereClause, ordering, limit, offset)

      getThirdPartyFeeProfiles.as(thirdPartyFeeProfileRowParser.*)

    }, s"Error while retrieving third party fee profiles by criteria: $criteria")

  def getThirdPartyFeeProfileRangesByFeeProfileId(thirdPartyFeeProfileId: String): DaoResponse[Seq[ThirdPartyFeeProfileRange]] =
    withConnection({ implicit cxn: Connection ⇒
      internalGetThirdPartyFeeRange(thirdPartyFeeProfileId)
    }, s"Failed to fetch ranges for third party fee profile $thirdPartyFeeProfileId")

  private[sql] def internalGetThirdPartyFeeProfile(id: String, activeOnly: Boolean = true)(implicit cxn: Connection): Option[ThirdPartyFeeProfile] = {
    val query = fetchByIdQuery(activeOnly).on(cId → id)
    val ranges = internalGetThirdPartyFeeRange(id)

    query.executeQuery().as(thirdPartyFeeProfileRowParser.singleOpt)
      .map { profile ⇒
        if (profile.calculationMethod.toLowerCase.contains("staircase"))
          profile.copy(ranges = Some(ranges))
        else
          profile
      }
  }

  private[sql] def internalGetThirdPartyFeeRange(id: String)(implicit cxn: Connection): Seq[ThirdPartyFeeProfileRange] = {
    val query = fetchRangesByProfileIdQuery.on(cId → id)

    query.executeQuery().as(thirdPartyFeeProfileRangesRowParser.*)
  }

}

object ThirdPartyFeeProfileSqlDao {

  val TableName = "third_party_fee_profiles"
  val TableAlias = "tpfp"

  val ThirdPartyFeeProfileRangesTable = "third_party_fee_profile_ranges"
  val ThirdPartyFeeProfileRangesAlias = "tpfpr"

  val CurrencyTable = "currencies"
  val CurrencyAlias = "c"

  //third_party_fee_profiles columns
  val cId = "id"
  val cTransactionType = "transaction_type"
  val cOtherParty = "other_party"
  val cProviderId = "provider_id"
  val cIsActive = "is_active"
  val cCalculationMethod = "calculation_method"
  val cMaxFee = "max_fee"
  val cMinFee = "min_fee"
  val cFeeAmount = "fee_amount"
  val cFeeRatio = "fee_ratio"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
  val cCurrencyId = "currency_id"
  val cDeletedAt = "deleted_at"

  //third_party_fee_profile_ranges columns
  val cThirdPartyFeeProfileId = "third_party_fee_profile_id"
  val cMax = "max"
  val cMin = "min"

  //currencies columns
  val cCurrencyCode = "currency_code"
  val cIsCurrencyActive = "is_active"
  val cCurrencyName = "currency_name"

  private val insertProfileQuery =
    SQL(s"INSERT INTO $TableName ($cTransactionType, $cProviderId, $cCalculationMethod, $cMaxFee, $cMinFee, " +
      s"$cFeeAmount, $cFeeRatio, $cCreatedBy, $cCreatedAt, $cUpdatedBy, $cUpdatedAt, $cCurrencyId) " +
      s"VALUES ({$cTransactionType}, {$cProviderId}, {$cCalculationMethod}, {$cMaxFee}, {$cMinFee}, {$cFeeAmount}, {$cFeeRatio}," +
      s"{$cCreatedBy}, {$cCreatedAt}, {$cUpdatedBy}, {$cUpdatedAt} , {$cCurrencyId})")

  private val insertRangesQuery =
    s"INSERT INTO $ThirdPartyFeeProfileRangesTable ($cThirdPartyFeeProfileId, $cMax, $cMin, $cFeeAmount, $cFeeRatio, $cCreatedAt, $cUpdatedAt) " +
      s"VALUES ({$cThirdPartyFeeProfileId}, {$cMax}, {$cMin}, {$cFeeAmount}, {$cFeeRatio}, now(), now())"

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

  private def fetchRangesByProfileIdQuery: SqlQuery = {
    val filters = s"WHERE $TableAlias.$cId = {$cId}"
    SQL(s"""${baseFindRanges(filters)}""".stripMargin)
  }

  private def findThirdPartyFeeProfilesByCriteriaQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(o ⇒ o.copy(underlying = o.underlying.map({
      case ord if ord.field === "other_party" ⇒ ord.copy(field = "providers.name")
      case ord ⇒ ord
    })).toString)

    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    SQL(s"""${baseFindByCriteria(filters)} $ordering $pagination""".stripMargin)
  }

  private def baseFindRanges(filters: String): String = {
    s"""SELECT $ThirdPartyFeeProfileRangesAlias.*
       |FROM $ThirdPartyFeeProfileRangesTable $ThirdPartyFeeProfileRangesAlias
       |JOIN $TableName $TableAlias
       |ON $ThirdPartyFeeProfileRangesAlias.$cThirdPartyFeeProfileId = $TableAlias.$cId
       |$filters
       |ORDER BY $ThirdPartyFeeProfileRangesAlias.$cMin""".stripMargin
  }

  private def fetchByIdQuery(isActiveOnly: Boolean): SqlQuery = {
    val filters = if (isActiveOnly) {
      s"""WHERE $TableAlias.$cId = {$cId} AND $TableAlias.$cIsActive = 1"""
    } else {
      s"""WHERE $TableAlias.$cId = {$cId}"""
    }
    SQL(s"""${baseFindByCriteria(filters)}""".stripMargin)
  }

  private def baseFindByCriteria(filters: String): String =
    s"$qCommonSelect $filters".stripMargin

  private def where(mayBeCriteria: Option[ThirdPartyFeeProfileCriteria]): String = {
    import cats.implicits._
    import SqlDao._
    mayBeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql(none, TableAlias.some)),
        criteria.provider.map(_.toSql(Provider.cName.some, ProviderSqlDao.TableName.some)),
        criteria.transactionType.map(_.toSql(none, TableAlias.some)),
        CriteriaField(cIsCurrencyActive, 1).toSql(none, Some(CurrencyAlias)).some).flatten.toSql

    }.getOrElse("")
  }

  private def getNamedParameter(dto: ThirdPartyFeeProfileToInsert, providerId: Int): Seq[NamedParameter] =

    Seq[NamedParameter](
      cTransactionType → dto.transactionType,
      cProviderId → providerId,
      cCalculationMethod → dto.calculationMethod,
      cMaxFee → dto.maxFee,
      cMinFee → dto.minFee,
      cFeeAmount → dto.feeAmount,
      cFeeRatio → dto.feeRatio,
      cCreatedBy → dto.createdBy,
      cCreatedAt → dto.createdAt,
      cUpdatedAt → dto.createdAt, //not nullable in db and same as created at on insertion
      cUpdatedBy → dto.createdBy, //not nullable in db and same as created by on insertion
      cCurrencyId → dto.currencyId)

  private def getNamedParameterForRange(thirdPartyFeeProfileId: String, dto: ThirdPartyFeeProfileRangeToInsert): Seq[NamedParameter] =

    Seq[NamedParameter](
      NamedParameter(cThirdPartyFeeProfileId, thirdPartyFeeProfileId),
      NamedParameter(cMax, dto.max),
      NamedParameter(cMin, dto.min),
      NamedParameter(cFeeAmount, dto.feeAmount),
      NamedParameter(cFeeRatio, dto.feeRatio))

  private val thirdPartyFeeProfileRowParser: RowParser[ThirdPartyFeeProfile] = (row: Row) ⇒ {
    Try {
      ThirdPartyFeeProfile(
        id = row[Int](cId).toString,
        transactionType = row[Option[String]](cTransactionType),
        provider = row[String](s"${ProviderSqlDao.TableName}.${Provider.cName}"),
        calculationMethod = row[String](cCalculationMethod),
        isActive = row[Int](cIsActive).toBoolean,
        maxFee = row[Option[BigDecimal]](cMaxFee),
        minFee = row[Option[BigDecimal]](cMinFee),
        feeAmount = row[Option[BigDecimal]](cFeeAmount),
        feeRatio = row[Option[BigDecimal]](cFeeRatio),
        ranges = None,
        currencyCode = row[String](cCurrencyCode),
        createdAt = row[LocalDateTime](cCreatedAt),
        createdBy = row[String](cCreatedBy),
        updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
        updatedBy = row[Option[String]](cUpdatedBy))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val thirdPartyFeeProfileRangesRowParser: RowParser[ThirdPartyFeeProfileRange] = (row: Row) ⇒ {
    Try {
      ThirdPartyFeeProfileRange(
        id = row[Int](cId).toString,
        thirdPartyFeeProfileId = row[Int](cThirdPartyFeeProfileId).toString,
        max = row[Option[BigDecimal]](cMax),
        min = row[Option[BigDecimal]](cMin),
        feeAmount = row[Option[BigDecimal]](cFeeAmount),
        feeRatio = row[Option[BigDecimal]](cFeeRatio))

    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }
}
