package tech.pegb.backoffice.dao.limit.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm._
import javax.inject.Inject

import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.Dao.{EntityId, IntEntityId, UUIDEntityId}
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.limit.abstraction.LimitProfileDao
import tech.pegb.backoffice.dao.limit.dto.{LimitProfileCriteria, LimitProfileToInsert, LimitProfileToUpdate}
import tech.pegb.backoffice.dao.limit.entity.LimitProfile
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.dao.provider.abstraction.ProviderDao
import tech.pegb.backoffice.dao.provider.dto.ProviderCriteria
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.mapping.domain.dao.Implicits.IntConverter
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.dao.util.Implicits._

import scala.util.Try

class LimitProfileSqlDao @Inject() (
    override protected val dbApi: DBApi,
    historyDao: LimitProfileHistorySqlDao,
    providerDao: ProviderDao,
    kafkaDBSyncService: KafkaDBSyncService)
  extends LimitProfileDao with MostRecentUpdatedAtGetter[LimitProfile, LimitProfileCriteria] with SqlDao {

  import LimitProfileSqlDao._
  import SqlDao._

  protected def getUpdatedAtColumn: String = s"${LimitProfileSqlDao.TableAlias}.${LimitProfileSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = LimitProfileSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ LimitProfile = (arg: Row) ⇒ convertRowToLimitProfile(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[LimitProfileCriteria]): String = getWhere(criteriaDto)

  override def getLimitProfile(id: EntityId): DaoResponse[Option[LimitProfile]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetLimitProfile(id)
    }, s"Failed to fetch limit profile $id")
  }

  override def getLimitProfileByCriteria(
    criteria: LimitProfileCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[LimitProfile]] = withConnection({ implicit connection ⇒

    val whereFilter = getWhere(criteria.toOption)
    val limitProfilesByCriteriaSql = findLimitProfilesByCriteriaQuery(whereFilter, ordering, limit, offset)
    limitProfilesByCriteriaSql.as(limitProfileRowParser.*)

  }, s"Error while retrieving limitProfiles by criteria:$criteria")

  override def countLimitProfileByCriteria(criteria: LimitProfileCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = getWhere(criteria.toOption)
    val countByCriteriaSql = countLimitProfilesByCriteriaQuery(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  override def insertLimitProfile(dto: LimitProfileToInsert): DaoResponse[LimitProfile] = {
    withTransactionAndFlatten({ implicit cxn ⇒

      for {
        providerId ← dto.provider match {
          case Some(providerName) ⇒
            providerDao.getByCriteria(ProviderCriteria(providerName), None, None, None)(Some(cxn)).map(_.headOption.map(_.id)).fold(
              err ⇒ Left(DaoError.GenericDbError(s"Failed to insert limit profile when fetching provider [${providerName}]. Reason: ${err.message}")),
              {
                case Some(provider) ⇒
                  Right(Some(provider))
                case None ⇒
                  Left(DaoError.EntityNotFoundError(s"Failed to insert limit profile because because provider [${providerName}] was not found."))
              })
          case None ⇒ Right(None)
        }

        generatedId ← Try {
          insertQuery
            .on(
              cLimitType → dto.limitType,
              cUserType → dto.userType,
              cTier → dto.tier,
              cSubscription → dto.subscription,
              cTransactionType → dto.transactionType,
              cChannel → dto.channel,
              cProviderId → providerId,
              cInstrument → dto.instrument,
              cMaxIntervalAmount → dto.maxIntervalAmount,
              cMaxAmount → dto.maxAmount,
              cMinAmount → dto.minAmount,
              cMaxCount → dto.maxCount,
              cInterval → dto.interval,
              cCurrencyId → dto.currencyId,
              cUuid → genId(),
              cCreatedBy → dto.createdBy,
              cCreatedAt → dto.createdAt,
              cUpdatedBy → dto.createdBy, //not nullable in db and same as created at on insertion
              cUpdatedAt → dto.createdAt) //not nullable in db and same as created at on insertion
            .executeInsert(SqlParser.scalar[Int].single)
        }.toEither.left.map(_ ⇒ genericDbError("Failed to insert limit profile"))

        maybeLimitProfile ← Try(internalGetLimitProfile(generatedId.asEntityId)).toEither
          .left.map(_ ⇒ genericDbError("Failed to fetch created limit profile"))

        limitProfile ← maybeLimitProfile match {
          case Some(limitProfile) ⇒ Right(limitProfile)
          case None ⇒ Left(genericDbError("Failed to fetch created limit profile"))
        }

        _ ← Try(kafkaDBSyncService.sendInsert(TableName, limitProfile)).toEither.fold(error ⇒ {
          logger.warn(s"Failed to produce created limit profile [${dto.toSmartString}] to kafka", error)
          Right(())
        }, _ ⇒ Right(()))

      } yield {

        limitProfile
      }

    }, s"Unexpected error while inserting limit profile")
  }

  //TODO soft delete should be handled in domain
  override def updateLimitProfile(id: EntityId, updateDto: LimitProfileToUpdate): DaoResponse[Option[LimitProfile]] = {
    withTransaction(
      block = { implicit cxn ⇒
        for {
          existing ← internalGetLimitProfile(id)
          updateResult = convertUpdateDtoToSqlQuery(id, updateDto).executeUpdate()
          updated ← if (updateResult.isUpdated) {
            internalGetLimitProfile(id, activeOnly = false)
          } else {
            throw new IllegalStateException(s"Update failed. Limit profile ${id} has been modified by another process.")
          }
          uBy = updateDto.updatedBy
          uAt = updateDto.updatedAt
          reason = updateDto.reason
          _ = historyDao.internalCreateHistoryRecord(existing, updated, uBy, uAt, reason)
        } yield {
          kafkaDBSyncService.sendUpdate(TableName, updated)
          updated
        }
      },
      errorMsg = s"Failed to update limit profile $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update LimitProfile ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  private[sql] def internalGetLimitProfile(
    entityId: EntityId,
    activeOnly: Boolean = true)(
    implicit
    cxn: Connection): Option[LimitProfile] = {
    val query = entityId match {
      case UUIDEntityId(uuid) ⇒
        (if (activeOnly) fetchActiveByUuidQuery else fetchByUuidQuery)
          .on(cUuid → uuid)
      case IntEntityId(id) ⇒
        (if (activeOnly) fetchActiveByIdQuery else fetchByIdQuery)
          .on(cId → id)
    }
    query.executeQuery().as(limitProfileRowParser.singleOpt)
  }
}

object LimitProfileSqlDao {
  val TableName = "limit_profiles"
  val TableAlias = "lp"

  val cId = "id"
  val cUuid = "uuid"
  val cLimitType = "limit_type"
  val cUserType = "user_type"
  val cTier = "tier"
  val cSubscription = "subscription"
  val cTransactionType = "transaction_type"
  val cChannel = "channel"
  val cProviderId = "provider_id"
  val cInstrument = "instrument"
  val cMaxIntervalAmount = "max_interval_amount"
  val cMaxAmount = "max_amount"
  val cMinAmount = "min_amount"
  val cMaxCount = "max_count"
  val cInterval = "interval"
  val cCurrencyId = "currency_id"
  val cDeletedAt = "deleted_at"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"

  val CurrencyTable = "currencies"
  val CurrencyAlias = "c"
  val cIsCurrencyActive = "is_active"
  val cCurrencyName = "currency_name"

  val cCurrencyCode = "currency_code"

  private val insertQuery =
    SQL(s"INSERT INTO $TableName ($cLimitType, $cUserType, $cTier, $cSubscription, $cTransactionType, `$cChannel`," +
      s"$cProviderId, $cInstrument, $cMaxIntervalAmount, $cMaxAmount, $cMinAmount, $cMaxCount, `$cInterval`," +
      s"$cCurrencyId, $cUuid, $cCreatedBy, $cCreatedAt, $cUpdatedBy, $cUpdatedAt) " +
      s"VALUES ({$cLimitType}, {$cUserType}, {$cTier}, {$cSubscription}, {$cTransactionType}, {$cChannel}," +
      s"{$cProviderId}, {$cInstrument}, {$cMaxIntervalAmount}, {$cMaxAmount}, {$cMinAmount}, {$cMaxCount}," +
      s"{$cInterval}, {$cCurrencyId}, {$cUuid}, {$cCreatedBy}, {$cCreatedAt}, {$cUpdatedBy}, {$cUpdatedAt})")

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

  private val fetchActiveByIdQuery = SQL(
    s"""$qCommonSelect WHERE $TableAlias.$cId = {$cId} AND $TableAlias.$cDeletedAt IS NULL
       |AND $CurrencyAlias.$cIsCurrencyActive = true""".stripMargin)

  private val fetchByIdQuery = SQL(
    s"""$qCommonSelect
       |WHERE $TableAlias.$cId = {$cId}
       |AND $CurrencyAlias.$cIsCurrencyActive = true""".stripMargin)

  private val fetchActiveByUuidQuery = SQL(
    s"""$qCommonSelect WHERE $TableAlias.$cUuid = {$cUuid} AND $TableAlias.$cDeletedAt IS NULL
       |AND $CurrencyAlias.$cIsCurrencyActive = true""".stripMargin)

  private val fetchByUuidQuery = SQL(
    s"""$qCommonSelect WHERE $TableAlias.$cUuid = {$cUuid}
       |AND $CurrencyAlias.$cIsCurrencyActive = true""".stripMargin)

  private def baseFindLimitProfilesByCriteria(selectColumns: String, filters: String): String = {

    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |JOIN $CurrencyTable $CurrencyAlias
       |ON $TableAlias.$cCurrencyId = $CurrencyAlias.$cId
       |
       |LEFT JOIN ${ProviderSqlDao.TableName}
       |ON $TableAlias.$cProviderId = ${ProviderSqlDao.TableName}.${Provider.cId}
       |
       |$filters""".stripMargin
  }

  private def countLimitProfilesByCriteriaQuery(filters: String): SqlQuery = {
    val column = "COUNT(*) as n"
    SQL(s"""${baseFindLimitProfilesByCriteria(column, filters)}""")
  }

  private def findLimitProfilesByCriteriaQuery(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    /*TODO make ordering to add table name with column name
    i.e approach to build map of col and table name and filtering out from ordering set*/
    //interval is reserved keyword in sql
    val ordering = maybeOrderBy.fold("")(o ⇒ o.copy(underlying = o.underlying.map({
      case ord if ord.field === cInterval ⇒ ord.copy(field = s"`$cInterval`")
      case ord if ord.field === "other_party" ⇒ ord.copy(field = s"${ProviderSqlDao.TableName}.${Provider.cName}")
      case ord ⇒ ord
    })).toString)

    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)
    val columns = s"$TableAlias.*, $CurrencyAlias.$cCurrencyName as $cCurrencyCode, ${ProviderSqlDao.TableName}.${Provider.cName}"
    SQL(
      s"""${baseFindLimitProfilesByCriteria(columns, filters)}
         |$ordering $pagination""".stripMargin)

  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def convertRowToLimitProfile(row: Row): LimitProfile =
    LimitProfile(
      id = row[Int](cId),
      uuid = row[UUID](cUuid),
      limitType = row[String](cLimitType),
      userType = row[String](cUserType),
      tier = row[String](cTier),
      subscription = row[String](cSubscription),
      transactionType = row[Option[String]](cTransactionType),
      channel = row[Option[String]](cChannel),
      provider = row[Option[String]](s"${ProviderSqlDao.TableName}.${Provider.cName}"),
      instrument = row[Option[String]](cInstrument),
      interval = row[Option[String]](cInterval),
      maxIntervalAmount = row[Option[BigDecimal]](cMaxIntervalAmount),
      maxAmount = row[Option[BigDecimal]](cMaxAmount),
      minAmount = row[Option[BigDecimal]](cMinAmount),
      maxCount = row[Option[Int]](cMaxCount),
      deletedAt = row[Option[LocalDateTime]](cDeletedAt),
      createdBy = row[String](cCreatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      currencyCode = row[String](cCurrencyCode))

  private val limitProfileRowParser: RowParser[LimitProfile] = (row: Row) ⇒ {
    Try {
      convertRowToLimitProfile(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Error in parsing row to Contact entity. Reason: ${exc.getMessage}"))),
      anorm.Success(_))
  }

  private def convertUpdateDtoToSqlQuery(entityId: EntityId, dto: LimitProfileToUpdate): SimpleSql[Row] = {
    val paramsBuilder = dto.paramsBuilder
    val filter = entityId match {
      case UUIDEntityId(uuid) ⇒
        paramsBuilder += cUuid → uuid
        s"WHERE $cUuid = {$cUuid}"
      case IntEntityId(id) ⇒
        paramsBuilder += cId → id
        s"WHERE $cId = {$cId}"
    }
    val preQuery = dto.createSqlString(TableName, Some(filter))
    val params = paramsBuilder.result()
    SQL(preQuery).on(params: _*)

  }

  private def getWhere(maybeCriteria: Option[LimitProfileCriteria]): String = {
    import SqlDao._

    maybeCriteria.map { criteria ⇒
      Seq(
        criteria.uuid.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.limitType.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.userType.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.tier.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.subscription.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.transactionType.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.channel.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.instrument.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.interval.map(_.toSql(tableAlias = TableAlias.toOption)),
        criteria.provider.map(_.toSql(column = Provider.cName.toOption, tableAlias = ProviderSqlDao.TableName.toOption)),
        criteria.isDeleted.map(d ⇒ toNullSql(cDeletedAt, !d.value, TableAlias.toOption)),
        criteria.currencyCode.map(_.toSql(tableAlias = CurrencyAlias.toOption)),
        Some(CriteriaField(cIsCurrencyActive, 1).toSql(tableAlias = CurrencyAlias.toOption))).flatten.toSql

    }.getOrElse("")

  }
}
