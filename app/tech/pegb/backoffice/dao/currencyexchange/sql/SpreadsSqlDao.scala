package tech.pegb.backoffice.dao.currencyexchange.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm.{NamedParameter, Row, SQL, SqlParser, SqlQuery}
import com.google.inject.Inject
import tech.pegb.backoffice.util.Implicits._
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.currencyexchange.abstraction.{CurrencyExchangeDao, SpreadsDao}
import tech.pegb.backoffice.dao.currencyexchange.dto.{SpreadCriteria, SpreadToInsert, SpreadUpdateDto}
import tech.pegb.backoffice.dao.currencyexchange.entity.Spread
import tech.pegb.backoffice.dao.model.{MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter

class SpreadsSqlDao @Inject() (
    val dbApi: DBApi, currencyExchangeDao: CurrencyExchangeDao, kafkaDBSyncService: KafkaDBSyncService) extends SpreadsDao with MostRecentUpdatedAtGetter[Spread, SpreadCriteria] with SqlDao {
  import SpreadsSqlDao._
  import SqlDao._

  override protected def getUpdatedAtColumn: String = s"${SpreadsSqlDao.TableAlias}.${SpreadsSqlDao.cUpdatedAt}"

  override protected def getMainSelectQuery: String =
    s"""SELECT $TableAlias.*,
       |$commonSelectedColumns
       |$commonJoinQuery""".stripMargin

  override protected def getRowToEntityParser: Row ⇒ Spread = (arg: Row) ⇒ SpreadsSqlDao.convertRowToSpread(arg)

  override protected def getWhereFilterFromCriteria(criteriaDto: Option[SpreadCriteria]): String = buildWhereFilter(criteriaDto)

  def getSpread(id: UUID): DaoResponse[Option[Spread]] =
    withConnection({ implicit connection: Connection ⇒
      findByIdQuery.on('uuid → id).as(findByIdQuery.defaultParser.singleOpt).map(convertRowToSpread)
    }, s"Error while retrieving spread by uuid:$id")

  def getSpreadsByCriteria(criteria: SpreadCriteria, ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Spread]] = withConnection({ implicit connection ⇒

    val whereClause = buildWhereFilter(criteria.toOption)

    val orderByClause = ordering.map(o ⇒
      o.underlying.map(o ⇒ s"${spreadsApiToTableColumnMapping(o.field)} ${o.order}").mkString("ORDER BY ", ", ", " ")).getOrElse("")

    val pagination = SqlDao.getPagination(limit, offset)

    val query = SQL(
      s"""SELECT $TableAlias.*,
         |$commonSelectedColumns
         |$commonJoinQuery
         |$whereClause $orderByClause $pagination""".stripMargin)

    query
      .as(query.defaultParser.*)
      .map(convertRowToSpread)

  }, s"Error while retrieving currencyExchanges by criteria:$criteria")

  def countSpreadsByCriteria(criteria: SpreadCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereClause = buildWhereFilter(criteria.toOption)
    val query = SQL(
      s"""SELECT COUNT(*) as n
         |$commonJoinQuery
         |$whereClause""".stripMargin)

    query
      .as(query.defaultParser.singleOpt)
      .map(row ⇒ row[Int]("n")).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def createSpread(spreadToInsert: SpreadToInsert): DaoResponse[Spread] = {
    val spreadId = genId()
    (for {
      currencyExchange ← currencyExchangeDao.findById(spreadToInsert.currencyExchangeId)
      createdSpread ← withConnectionAndFlatten(
        { implicit connection ⇒
          insertSql.on(buildParametersForSpread(currencyExchange.id, spreadId.toString, spreadToInsert): _*)
            .executeInsert(SqlParser.scalar[Long].single)
          getSpread(spreadId)
        }, s"Error in creating Spread")
    } yield {
      createdSpread.foreach { spread ⇒
        kafkaDBSyncService.sendInsert(TableName, spread)
      }
      createdSpread
    }).fold(
      daoError ⇒ Left(daoError),
      spreadOption ⇒ spreadOption.toRight(DaoError.EntityNotFoundError(s"Couldn't find newly created spread $spreadId")))
  }

  override def update(id: UUID, dto: SpreadUpdateDto)(implicit requestId: UUID): DaoResponse[Spread] = {
    withTransactionAndFlatten({ implicit cxn ⇒
      val fetchSpreadQuery = findByIdQuery.on('uuid → id)
      val paramBuilder = dto.paramsBuilder
      paramBuilder += cUuid → id
      for {
        existing ← fetchSpreadQuery
          .as(findByIdQuery.defaultParser.singleOpt)
          .map(convertRowToSpread)
          .toRight(entityNotFoundError(s"Spread $id couldn't be found"))
        updateResult = SQL(dto.createSqlString(TableName, Some(s"WHERE $cUuid = {$cUuid}")))
          .on(paramBuilder.result(): _*)
          .executeUpdate()
        _ = makeHistoryRecord(
          spreadId = existing.id,
          oldSpread = existing.spread,
          newSpread = dto.spread,
          reason = dto.deletedAt.fold("upd")(_ ⇒ "rm"), // TODO: reason must come from api
          updatedBy = dto.updatedBy,
          updatedAt = dto.updatedAt)
        updated ← if (updateResult > 0) {
          dto.deletedAt.fold(fetchSpreadQuery
            .as(findByIdQuery.defaultParser.singleOpt)
            .map(convertRowToSpread)
            .toRight(entityNotFoundError(s"Spread $id couldn't be found")))(_ ⇒ Right(existing))
        } else {
          throw new IllegalStateException(s"Update failed. Spread ${id} has been modified by another process.")
        }
      } yield {
        kafkaDBSyncService.sendUpdate(TableName, updated)
        updated
      }
    }, s"Failed to perform update of spread $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update FeeProfile ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  @deprecated("use update with deletedAt set in SpreadUpdateDto", "")
  override def delete(id: UUID, dAt: LocalDateTime, uBy: String)(implicit requestId: UUID): DaoResponse[Spread] = {
    withTransactionAndFlatten({ implicit cxn ⇒
      for {
        existing ← findByIdQuery
          .on('uuid → id)
          .as(findByIdQuery.defaultParser.singleOpt)
          .map(row ⇒ {
            deleteQuery
              .on('id → id, 'dAt → dAt)
              .executeUpdate()
            convertRowToSpread(row)
          })
          .toRight(entityNotFoundError(s"Spread id couldn't be found"))
        _ = makeHistoryRecord(
          spreadId = existing.id,
          oldSpread = existing.spread,
          newSpread = existing.spread,
          reason = "rm",
          updatedBy = uBy,
          updatedAt = dAt)
      } yield {
        kafkaDBSyncService.sendDelete(TableName, existing.id)
        existing
      }
    }, s"Failed to perform update of spread $id")
  }

  //TODO: Do we need historic records on greenplum?
  private def makeHistoryRecord(
    spreadId: Int,
    oldSpread: BigDecimal,
    newSpread: BigDecimal,
    reason: String,
    updatedBy: String,
    updatedAt: LocalDateTime)(implicit cxn: Connection): Option[Long] = {
    addHistoryRecordQuery
      .on(
        'spreadId → spreadId,
        'newSpread → newSpread,
        'oldSpread → oldSpread,
        'reason → reason,
        'uBy → updatedBy,
        'uAt → updatedAt)
      .executeInsert[Option[Long]]()
  }
}

object SpreadsSqlDao {

  val TableName = "currency_spreads"
  val TableAlias = "cp"
  val currencyExchangeTableAlias = "fx"

  val spreadsApiToTableColumnMapping = Map(
    "id" → "uuid",
    "currency" → "currency_rate_currency_code")
    .withDefault(identity)

  val cId = "id"
  val cUuid = "uuid"
  val cCurrRateId = "currency_rate_id"
  val cCurrRateUuid = "currency_rate_uuid"
  val cCurrRateCurrCode = "currency_rate_currency_code"
  val cTxnType = "transaction_type"
  val cChannel = "channel"
  val cInstitution = "institution"
  val cSpread = "spread"
  val cDeletedAt = "deleted_at"
  val cCreatedAt = "created_at"
  val cCreatedBy = "created_by"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"

  val commonSelectedColumns =
    s"""
       |${CurrencyExchangeSqlDao.TableAlias}.${CurrencyExchangeSqlDao.cUuid} as $cCurrRateUuid,
       |${AccountSqlDao.CurrencyTblAlias}.${AccountSqlDao.CurrencyTblCurrencyCode} as $cCurrRateCurrCode
     """.stripMargin

  val commonJoinQuery =
    s"""FROM $TableName $TableAlias
       |JOIN ${CurrencyExchangeSqlDao.TableName} ${CurrencyExchangeSqlDao.TableAlias}
       |ON $TableAlias.$cCurrRateId = ${CurrencyExchangeSqlDao.TableAlias}.${CurrencyExchangeSqlDao.cId}
       |JOIN ${AccountSqlDao.CurrencyTblName} ${AccountSqlDao.CurrencyTblAlias}
       |ON ${CurrencyExchangeSqlDao.TableAlias}.${CurrencyExchangeSqlDao.cCurrencyId} = ${AccountSqlDao.CurrencyTblAlias}.${AccountSqlDao.CurrencyTblId}
       |""".stripMargin

  private[dao] def insertSql = {
    SQL(
      s"""INSERT INTO $TableName ($cUuid, $cCurrRateId, $cTxnType, $cChannel, $cInstitution, $cSpread, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy) VALUES
         |({$cUuid}, {$cCurrRateId}, {$cTxnType}, {$cChannel}, {$cInstitution}, {$cSpread}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy});""".stripMargin)
  }

  def buildParametersForSpread(customerExchangeId: Long, spreadUUID: String, spreadToInsert: SpreadToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      cUuid → spreadUUID,
      cCurrRateId → customerExchangeId,
      cTxnType → spreadToInsert.transactionType,
      cChannel → spreadToInsert.channel,
      cInstitution → spreadToInsert.institution,
      cSpread → spreadToInsert.spread,
      cCreatedAt → spreadToInsert.createdAt,
      cCreatedBy → spreadToInsert.createdBy,
      cUpdatedAt → spreadToInsert.createdAt, //not nullable in db and same as created at on insertion
      cUpdatedBy → spreadToInsert.createdBy) //not nullable in db and same as created by on insertion

  val findByIdQuery: SqlQuery = {
    SQL(
      s"""
         |SELECT $TableAlias.*,
         |$commonSelectedColumns
         |$commonJoinQuery
         |WHERE $TableAlias.$cUuid = {uuid} AND $TableAlias.$cDeletedAt IS NULL
         """.stripMargin)
  }

  val updateQuery: SqlQuery = SQL(
    s"""UPDATE $TableName
       |SET $cSpread = {$cSpread}, $cUpdatedBy = {uBy}, $cUpdatedAt = {uAt}
       |WHERE $cUuid = {id}""".stripMargin)

  val addHistoryRecordQuery: SqlQuery = SQL(
    s"""INSERT INTO currency_spreads_history(spread_id, new_spread, old_spread,
       |reason, updated_by, updated_at) VALUES
       |({spreadId}, {newSpread}, {oldSpread},
       |{reason}, {uBy}, {uAt})""".stripMargin)

  val deleteQuery: SqlQuery = SQL(s"UPDATE $TableName SET $cDeletedAt = {dAt} WHERE $cUuid = {id};")

  private def convertRowToSpread(row: Row) = {
    Spread(
      id = row[Int](cId),
      uuid = row[UUID](cUuid),
      currencyExchangeId = row[Int](cCurrRateId),
      currencyExchangeUuid = row[UUID](cCurrRateUuid),
      transactionType = row[String](cTxnType),
      channel = row[Option[String]](cChannel),
      recipientInstitution = row[Option[String]](cInstitution),
      spread = row[BigDecimal](cSpread),
      deletedAt = row[Option[LocalDateTime]](cDeletedAt),
      createdBy = row[String](cCreatedBy),
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedBy = row[Option[String]](cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))
  }

  private def buildWhereFilter(mayBeCriteria: Option[SpreadCriteria]): String = {
    import SqlDao._

    mayBeCriteria.map { criteria ⇒
      val spreadUuidFilter = criteria.id.map { cf ⇒
        queryConditionClause(cf.value, cUuid, Some(TableAlias), cf.operator == MatchTypes.Partial)
      }

      val currencyExchangeUuidFilter = criteria.currencyExchangeId.map { cf ⇒
        queryConditionClause(cf.value, CurrencyExchangeSqlDao.cUuid, Some(currencyExchangeTableAlias), cf.operator == MatchTypes.Partial)
      }

      val currencyCodeFilter = criteria.currencyCode.map(
        queryConditionClause(_, CurrencySqlDao.cName, Some(AccountSqlDao.CurrencyTblAlias)))

      val transactionTypeFilter = criteria.transactionType.map(
        queryConditionClause(_, cTxnType, Some(TableAlias)))

      val channelFilter = criteria.channel.map(
        queryConditionClause(_, cChannel, Some(TableAlias)))

      val recipientInstitutionFilter = criteria.recipientInstitution.map(
        queryConditionClause(_, cInstitution, Some(TableAlias)))

      val deletedAtFilter = criteria.isDeletedAtNotNull.map(notNull ⇒
        toNullSql(cDeletedAt, !notNull, Some(TableAlias)))

      val filters = Seq(spreadUuidFilter, currencyExchangeUuidFilter, currencyCodeFilter,
        transactionTypeFilter, channelFilter, recipientInstitutionFilter, deletedAtFilter)
        .flatten.mkString(" AND ")

      if (filters.nonEmpty) s"WHERE $filters"
      else ""
    }
  }.getOrElse("")
}
