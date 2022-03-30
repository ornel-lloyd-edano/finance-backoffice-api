package tech.pegb.backoffice.dao.transaction.sql

import java.time.{LocalDateTime, ZoneOffset}

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.transaction.abstraction.SettlementDao
import tech.pegb.backoffice.dao.transaction.dto.{SettlementCriteria, SettlementFxHistoryCriteria, SettlementRecentAccountCriteria, SettlementToInsert}
import tech.pegb.backoffice.dao.transaction.entity
import tech.pegb.backoffice.dao.transaction.entity.{SettlementFxHistory, SettlementRecentAccount}
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class SettlementSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig,
    kafkaDBSyncService: KafkaDBSyncService)
  extends SettlementDao with SqlDao {

  import SettlementSqlDao._
  import SqlDao._

  override def countSettlementsByCriteria(criteria: SettlementCriteria): DaoResponse[Int] = {
    withConnection({ implicit connection ⇒
      val whereFilter = generateWhereFilter(criteria)

      val query = SQL(
        s"""
           |$baseCountQuery
           |$whereFilter
         """.stripMargin)

      query
        .as(query.defaultParser.singleOpt)
        .map(SettlementSqlDao.parseRowToCount(_)).getOrElse(0)

    }, s"Error while retrieving count manual transactions by criteria:$criteria")

  }

  override def getSettlementsByCriteria(
    criteria: SettlementCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[entity.Settlement]] = {

    withConnection({ implicit connection ⇒
      val whereFilter = generateWhereFilter(criteria)
      val ord = {
        val ord = orderBy.map(_.toString).toSql
        if (ord.isEmpty)
          //provide implicit ordering for deterministic results
          s"ORDER BY ${Settlements.tableAlias}.${Settlements.colId} ASC, ${SettlementLines.tableAlias}.${SettlementLines.colId} ASC"
        else ord
      }
      val pagination = getPagination(limit, offset)

      val query = SQL(
        s"""
           |$baseSelectQuery
           |$whereFilter
           |$ord $pagination
         """.stripMargin)

      val result = query
        .as(query.defaultParser.*)
        .map(SettlementSqlDao.parseRowToEntity(_))
        .groupBy(pair ⇒ pair._1) //groupBy fucks up ordering, fix this by doing it in the anorm level
        .map(group ⇒ {
          group._1.copy(
            lines = group._2.map(_._2).sortBy(_.id) /*groupBy fucks up ordering, quick fix for now*/ )
        })

      result.toSeq.sortBy(_.id) //groupBy fucks up ordering, quick fix for now
    }, s"Error while retrieving manual transactions by criteria:$criteria")
  }

  override def insertSettlement(dto: SettlementToInsert): DaoResponse[entity.Settlement] = {
    for {
      generatedId ← withTransaction({ implicit conn ⇒
        val generatedId = insertSettlementQuery.on(
          Settlements.colUuid → dto.uuid,
          Settlements.colReason → dto.reason,
          Settlements.colStatus → dto.status,
          Settlements.colCreatedBy → dto.createdBy,
          Settlements.colCreatedAt → dto.createdAt,
          Settlements.colCheckedBy → dto.checkedBy,
          Settlements.colCheckedAt → dto.checkedAt,
          Settlements.colFxProvider → dto.fxProvider,
          Settlements.colFromCurrencyId → dto.fromCurrencyId,
          Settlements.colToCurrencyId → dto.toCurrencyId,
          Settlements.colFxRate → dto.fxRate,
          Settlements.colUpdatedAt → LocalDateTime.now(ZoneOffset.UTC)).executeInsert(SqlParser.scalar[Int].single)

        dto.settlementLines.map(line ⇒ {
          insertSettlementLinesQuery.on(
            SettlementLines.colSettlementId → generatedId,
            SettlementLines.colAccountId → line.accountId,
            SettlementLines.colDirection → line.direction,
            SettlementLines.colCurrencyId → line.currencyId,
            SettlementLines.colAmount → line.amount,
            SettlementLines.colExplanation → line.explanation,
            SettlementLines.colUpdatedAt → LocalDateTime.now(ZoneOffset.UTC)).executeInsert(SqlParser.scalar[Int].single)
        })
        generatedId
      }, s"Error while inserting settlement and/or settlement lines.")

      insertedEntity ← getSettlementsByCriteria(SettlementCriteria(id = Some(generatedId)), None, None, None)
        .fold(
          error ⇒ Left(error),
          maybeInsertedEntity ⇒
            Try(maybeInsertedEntity.head).toEither.fold(_ ⇒ Left(DaoError.EntityNotFoundError("Inserted settlement entity was not found")), entity ⇒ Right(entity)))
    } yield {
      kafkaDBSyncService.sendUpdate(Settlements.tableName, insertedEntity)
      insertedEntity
    }
  }

  def getSettlementFxHistory(
    criteria: SettlementFxHistoryCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[SettlementFxHistory]] = withConnection({ implicit connection ⇒
    val whereFilter = generateFxHistoryWhereFilter(criteria.toOption)

    val order = orderBy.fold("")(_.toString)
    val pagination = SqlDao.getPagination(limit, offset)

    val columns =
      s"""${Settlements.tableAlias}.*,
         |${Currencies.tableAliasFrom}.${Currencies.colCurrencyName} as ${Settlements.aliasFromCurrency},
         |${Currencies.tableAliasFrom}.${Currencies.colIcon} as ${Settlements.aliasFromIcon},
         |${Currencies.tableAliasTo}.${Currencies.colCurrencyName} as ${Settlements.aliasToCurrency},
         |${Currencies.tableAliasTo}.${Currencies.colIcon} as ${Settlements.aliasToIcon}""".stripMargin

    val settlementFxHistorySql = SQL(s"""${baseSelectSettlementFxHistory(columns, whereFilter)} $order $pagination""".stripMargin)

    settlementFxHistorySql.as(settlementFxHistoryRowParser.*)
  }, s"Error while retrieving SettlementFxHistory by criteria: $criteria")

  def countSettlementFxHistory(criteria: SettlementFxHistoryCriteria): DaoResponse[Int] = {
    withConnection({ implicit connection ⇒
      val whereFilter = generateFxHistoryWhereFilter(criteria.toOption)

      val column = "COUNT(*) as n"

      val countByCriteriaSql = SQL(s"""${baseSelectSettlementFxHistory(column, whereFilter)}""")

      countByCriteriaSql
        .as(countByCriteriaSql.defaultParser.singleOpt)
        .map(row ⇒ parseRowToCount(row)).getOrElse(0)

    }, s"Error while retrieving count manual transactions by criteria:$criteria")

  }

  def getSettlementRecentAccounts(
    criteria: SettlementRecentAccountCriteria,
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[SettlementRecentAccount]] = withConnection({ implicit connection ⇒
    val whereFilter = generateRecentAccountWhereFilter(criteria.toOption)

    val pagination = SqlDao.getPagination(limit, offset)

    val columns =
      s"""${SettlementLines.tableAlias}.${SettlementLines.colAccountId},
         |${SettlementLines.tableAlias}.${SettlementLines.colUpdatedAt},
         |${Accounts.tableAlias}.*,
         |${Currencies.tableAliasGeneric}.*,
         |${Users.tableAlias}.*,
         |${IndividualUsers.tableAlias}.*,
         |${BusinessUsers.tableAlias}.*""".stripMargin

    val order = s"ORDER BY ${SettlementLines.tableAlias}.${SettlementLines.colUpdatedAt} DESC"

    val settlementRecentAccountsSql = SQL(s"""${baseSelectRecentAccount(columns, whereFilter)} $order $pagination""".stripMargin)

    settlementRecentAccountsSql.as(settlementRecentAccountsRowParser.*)
  }, s"Error while retrieving SettlementRecentAccounts by criteria: $criteria")
}

object SettlementSqlDao {

  val baseCountQuery =
    s"""
       |SELECT COUNT(DISTINCT ${Settlements.tableAlias}.${Settlements.colId}) as ${SqlDao.countAlias}
       |${commonJoinQuery}
     """.stripMargin

  val baseSelectQuery =
    s"""
       |SELECT ${Settlements.tableAlias}.*, ${SettlementLines.tableAlias}.*,
       |${AccountSqlDao.TableAlias}.${AccountSqlDao.cNumber},
       |${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName}
       |${commonJoinQuery}
     """.stripMargin

  def baseSelectSettlementFxHistory(selectColumns: String, filters: String): String = {
    s"""
       |SELECT $selectColumns
       |FROM ${Settlements.tableName} ${Settlements.tableAlias}
       |INNER JOIN ${Currencies.tableName} ${Currencies.tableAliasFrom}
       |ON ${Settlements.tableAlias}.${Settlements.colFromCurrencyId} = ${Currencies.tableAliasFrom}.${Currencies.colId}
       |INNER JOIN ${Currencies.tableName} ${Currencies.tableAliasTo}
       |ON ${Settlements.tableAlias}.${Settlements.colToCurrencyId} = ${Currencies.tableAliasTo}.${Currencies.colId}
       |$filters
     """.stripMargin
  }

  def baseSelectRecentAccount(selectColumns: String, filters: String): String = {
    s"""
       |SELECT $selectColumns
       |FROM ${SettlementLines.tableName} ${SettlementLines.tableAlias}
       |JOIN ${Accounts.tableName} ${Accounts.tableAlias}
       |ON ${SettlementLines.tableAlias}.${SettlementLines.colAccountId} = ${Accounts.tableAlias}.${Accounts.colId}
       |AND ${Accounts.tableAlias}.${Accounts.colStatus} = 'active'
       |JOIN ${Currencies.tableName} ${Currencies.tableAliasGeneric}
       |ON ${Accounts.tableAlias}.${Accounts.colCurrencyId} = ${Currencies.tableAliasGeneric}.${Currencies.colId}
       |JOIN ${Users.tableName} ${Users.tableAlias}
       |ON ${Users.tableAlias}.${Users.colId} = ${Accounts.tableAlias}.${Accounts.colUserId}
       |LEFT JOIN ${IndividualUsers.tableName} ${IndividualUsers.tableAlias}
       |ON ${Users.tableAlias}.${Users.colId} = ${IndividualUsers.tableAlias}.${IndividualUsers.colUserId}
       |LEFT JOIN ${BusinessUsers.tableName} ${BusinessUsers.tableAlias}
       |ON ${Users.tableAlias}.${Users.colId} = ${BusinessUsers.tableAlias}.${BusinessUsers.colUserId}
       |$filters
     """.stripMargin
  }

  lazy val commonJoinQuery =
    s"""
       |FROM ${Settlements.tableName} as ${Settlements.tableAlias}
       |JOIN ${SettlementLines.tableName} as ${SettlementLines.tableAlias}
       |ON ${Settlements.tableAlias}.${Settlements.colId} = ${SettlementLines.tableAlias}.${SettlementLines.colSettlementId}
       |JOIN ${AccountSqlDao.TableName} as ${AccountSqlDao.TableAlias}
       |ON ${SettlementLines.tableAlias}.${SettlementLines.colAccountId} = ${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}
       |JOIN ${CurrencySqlDao.TableName} as ${CurrencySqlDao.TableAlias}
       |ON ${SettlementLines.tableAlias}.${SettlementLines.colCurrencyId} = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
     """.stripMargin

  val insertSettlementQuery =
    SQL(s"""
       |INSERT INTO ${Settlements.tableName}
       |(${Settlements.colUuid}, ${Settlements.colReason}, ${Settlements.colStatus}, ${Settlements.colCreatedBy},
       |${Settlements.colCreatedAt}, ${Settlements.colCheckedBy}, ${Settlements.colCheckedAt}, ${Settlements.colUpdatedAt},
       |${Settlements.colFxProvider}, ${Settlements.colFromCurrencyId}, ${Settlements.colToCurrencyId}, ${Settlements.colFxRate})
       |VALUES
       |({${Settlements.colUuid}}, {${Settlements.colReason}}, {${Settlements.colStatus}}, {${Settlements.colCreatedBy}},
       |{${Settlements.colCreatedAt}}, {${Settlements.colCheckedBy}}, {${Settlements.colCheckedAt}}, {${Settlements.colUpdatedAt}},
       |{${Settlements.colFxProvider}}, {${Settlements.colFromCurrencyId}}, {${Settlements.colToCurrencyId}}, {${Settlements.colFxRate}})
     """.stripMargin)

  val insertSettlementLinesQuery =
    SQL(s"""
       |INSERT INTO ${SettlementLines.tableName}
       |(${SettlementLines.colSettlementId}, ${SettlementLines.colAccountId}, ${SettlementLines.colDirection},
       |${SettlementLines.colCurrencyId}, ${SettlementLines.colAmount}, ${SettlementLines.colExplanation}, ${SettlementLines.colUpdatedAt})
       |VALUES
       |({${SettlementLines.colSettlementId}}, {${SettlementLines.colAccountId}}, {${SettlementLines.colDirection}},
       |{${SettlementLines.colCurrencyId}}, {${SettlementLines.colAmount}}, {${SettlementLines.colExplanation}},
       |{${SettlementLines.colUpdatedAt}})
     """.stripMargin)

  private def generateWhereFilter(criteria: SettlementCriteria) = {
    import SqlDao._

    val filters = Seq(
      criteria.id.map(queryConditionClause(_, Settlements.colId, Some(Settlements.tableAlias))),

      criteria.uuid.map(queryConditionClause(_, Settlements.colUuid, Some(Settlements.tableAlias))),

      fromDateTimeRange(Settlements.tableAlias, Settlements.colCreatedAt, criteria.createdAtFrom, criteria.createdAtTo),

      criteria.accountNumber
        .map(queryConditionClause(_, AccountSqlDao.cNumber, Some(AccountSqlDao.TableAlias))),

      criteria.currency
        .map(queryConditionClause(_, CurrencySqlDao.cName, Some(CurrencySqlDao.TableAlias))),

      criteria.direction
        .map(queryConditionClause(_, SettlementLines.colDirection, Some(SettlementLines.tableAlias))))

      .flatten.mkString(" AND ")

    if (filters.nonEmpty) s"WHERE $filters"
    else ""

  }

  private def generateFxHistoryWhereFilter(mayBeCriteria: Option[SettlementFxHistoryCriteria]) = {
    import SqlDao._

    mayBeCriteria.map { criteria ⇒
      Seq(
        Option(toNullSql(Settlements.colFxProvider, false, Some(Settlements.tableAlias))),
        Option(toNullSql(Settlements.colFxRate, false, Some(Settlements.tableAlias))),

        criteria.fxProvider.map(queryConditionClause(_, Settlements.colFxProvider, Some(Settlements.tableAlias))),
        criteria.fromCurrency.map(queryConditionClause(_, Currencies.colCurrencyName, Some(Currencies.tableAliasFrom))),
        criteria.toCurrency.map(queryConditionClause(_, Currencies.colCurrencyName, Some(Currencies.tableAliasTo))),
        fromDateTimeRange(Settlements.tableAlias, Settlements.colCreatedAt, criteria.createdAtFrom, criteria.createdAtTo))
        .flatten.toSql
    }.getOrElse("")

  }

  private def generateRecentAccountWhereFilter(mayBeCriteria: Option[SettlementRecentAccountCriteria]) = {
    import SqlDao._

    val maxSubQuery =
      s"""(SELECT MAX(sub_sl.${SettlementLines.colUpdatedAt})
         |FROM ${SettlementLines.tableName} sub_sl
         |JOIN ${Settlements.tableName} sub_s
         |ON sub_sl.${SettlementLines.colSettlementId} = sub_s.${Settlements.colId}
         |AND sub_s.${Settlements.colFxProvider} IS NOT NULL
         |WHERE sub_sl.${SettlementLines.colAccountId} = ${SettlementLines.tableAlias}.${SettlementLines.colAccountId})"""
        .stripMargin

    mayBeCriteria.map { criteria ⇒
      Seq(
        Option(s"${SettlementLines.tableAlias}.${SettlementLines.colUpdatedAt} = $maxSubQuery"),
        criteria.currency.map(queryConditionClause(_, Currencies.colCurrencyName, Some(Currencies.tableAliasGeneric))))
        .flatten.toSql
    }.getOrElse("")

  }

  def parseRowToEntity(row: Row) = {
    (entity.Settlement(
      id = row[Int](s"${Settlements.tableName}.${Settlements.colId}"),
      uuid = row[String](s"${Settlements.tableName}.${Settlements.colUuid}"),
      transactionReason = row[String](s"${Settlements.tableName}.${Settlements.colReason}"),
      status = row[String](s"${Settlements.tableName}.${Settlements.colStatus}"),
      createdBy = row[String](s"${Settlements.tableName}.${Settlements.colCreatedBy}"),
      createdAt = row[LocalDateTime](s"${Settlements.tableName}.${Settlements.colCreatedAt}"),
      checkedBy = row[Option[String]](s"${Settlements.tableName}.${Settlements.colCheckedBy}"),
      checkedAt = row[Option[LocalDateTime]](s"${Settlements.tableName}.${Settlements.colCheckedAt}"),
      fxProvider = row[Option[String]](s"${Settlements.tableName}.${Settlements.colFxProvider}"),
      fromCurrencyId = row[Option[Int]](s"${Settlements.tableName}.${Settlements.colFromCurrencyId}"),
      toCurrencyId = row[Option[Int]](s"${Settlements.tableName}.${Settlements.colToCurrencyId}"),
      fxRate = row[Option[BigDecimal]](s"${Settlements.tableName}.${Settlements.colFxRate}"),
      lines = Seq.empty), entity.SettlementLines(
        id = row[Int](s"${SettlementLines.tableName}.${SettlementLines.colId}"),
        manualSettlementId = row[Int](s"${SettlementLines.tableName}.${SettlementLines.colSettlementId}"),
        accountId = row[Int](s"${SettlementLines.tableName}.${SettlementLines.colAccountId}"),
        accountNumber = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cNumber}"),
        direction = row[String](s"${SettlementLines.tableName}.${SettlementLines.colDirection}"),
        currencyId = row[Int](s"${SettlementLines.tableName}.${SettlementLines.colCurrencyId}"),
        currency = row[String](s"${CurrencySqlDao.TableName}.${CurrencySqlDao.cName}"),
        amount = row[BigDecimal](s"${SettlementLines.tableName}.${SettlementLines.colAmount}"),
        explanation = row[String](s"${SettlementLines.tableName}.${SettlementLines.colExplanation}")))
  }

  def parseRowToCount(row: Row): Int = row[Int](s"${SqlDao.countAlias}")

  private val settlementFxHistoryRowParser: RowParser[SettlementFxHistory] = (row: Row) ⇒ {
    Try {
      SettlementFxHistory(
        fxProvider = row[String](s"${Settlements.tableName}.${Settlements.colFxProvider}"),
        fromCurrencyId = row[Int](s"${Settlements.tableName}.${Settlements.colFromCurrencyId}"),
        fromCurrency = row[String](s"${Settlements.aliasFromCurrency}"),
        fromIcon = row[String](s"${Settlements.aliasFromIcon}"),
        toCurrencyId = row[Int](s"${Settlements.tableName}.${Settlements.colToCurrencyId}"),
        toCurrency = row[String](s"${Settlements.aliasToCurrency}"),
        toIcon = row[String](s"${Settlements.aliasToIcon}"),
        fxRate = row[BigDecimal](s"${Settlements.tableName}.${Settlements.colFxRate}"),
        createdAt = row[LocalDateTime](s"${Settlements.tableName}.${Settlements.colCreatedAt}"))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val settlementRecentAccountsRowParser: RowParser[SettlementRecentAccount] = (row: Row) ⇒ {
    Try {
      val individualUserIdOption = row[Option[Int]](s"${IndividualUsers.tableName}.${IndividualUsers.colId}")
      val businessUserIdOption = row[Option[Int]](s"${BusinessUsers.tableName}.${BusinessUsers.colId}")

      SettlementRecentAccount(
        accountId = row[Int](s"${SettlementLines.tableName}.${SettlementLines.colAccountId}"),
        accountUUID = row[String](s"${Accounts.tableName}.${Accounts.colUUID}"),
        accountNumber = row[String](s"${Accounts.tableName}.${Accounts.colNumber}"),
        accountName = row[Option[String]](s"${Accounts.tableName}.${Accounts.colName}"),
        balance = row[BigDecimal](s"${Accounts.tableName}.${Accounts.colBalance}"),
        currency = row[String](s"${Currencies.tableName}.${Currencies.colCurrencyName}"),
        customerName = (individualUserIdOption, businessUserIdOption) match {
          case (Some(_), None) ⇒
            row[Option[String]](s"${IndividualUsers.tableName}.${IndividualUsers.colFullname}")
              .orElse(row[Option[String]](s"${IndividualUsers.tableName}.${IndividualUsers.colName}"))
          case (None, Some(_)) ⇒
            row[Option[String]](s"${BusinessUsers.tableName}.${BusinessUsers.colBusinessName}")
              .orElse(row[Option[String]](s"${BusinessUsers.tableName}.${BusinessUsers.colBrandName}"))
          case _ ⇒
            row[Option[String]](s"${Users.tableName}.${Users.colUsername}")
        })

    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  object Settlements {
    val tableName = "settlements"
    val tableAlias = "s1"
    val colId = "id"
    val colUuid = "uuid"
    val colReason = "reason"
    val colStatus = "status"
    val colFxProvider = "fx_provider"
    val colFromCurrencyId = "from_currency_id"
    val colToCurrencyId = "to_currency_id"
    val colFxRate = "fx_rate"
    val colCreatedBy = "created_by"
    val colCreatedAt = "created_at"
    val colCheckedBy = "checked_by"
    val colCheckedAt = "checked_at"
    val colUpdatedAt = "updated_at"

    val aliasFromCurrency = "from_currency"
    val aliasToCurrency = "to_currency"
    val aliasFromIcon = "from_icon"
    val aliasToIcon = "to_icon"
  }

  object Currencies {
    val tableName = "currencies"

    val tableAliasGeneric = "c"
    val tableAliasFrom = "c1"
    val tableAliasTo = "c2"

    val colId = "id"
    val colCurrencyName = "currency_name"
    val colIcon = "icon"
  }

  object Accounts {
    val tableName = "accounts"
    val tableAlias = "a"

    val colId = "id"
    val colUUID = "uuid"
    val colUserId = "user_id"
    val colNumber = "number"
    val colName = "name"
    val colCurrencyId = "currency_id"
    val colStatus = "status"
    val colBalance = "balance"
  }

  object Users {
    val tableName = "users"
    val tableAlias = "u"

    val colId = "id"
    val colUsername = "username"
  }

  object IndividualUsers {
    val tableName = "individual_users"
    val tableAlias = "iu"

    val colId = "id"
    val colUserId = "user_id"
    val colName = "name"
    val colFullname = "fullname"
  }

  object BusinessUsers {
    val tableName = "business_users"
    val tableAlias = "bu"

    val colId = "id"
    val colUserId = "user_id"
    val colBusinessName = "business_name"
    val colBrandName = "brand_name"
  }

  object SettlementLines {
    val tableName = "settlement_lines"
    val tableAlias = "s2"
    val colId = "id"
    val colSettlementId = "settlement_id"
    val colAccountId = "account_id"
    val colDirection = "direction"
    val colCurrencyId = "currency_id"
    val colAmount = "amount"
    val colExplanation = "explanation"
    val colUpdatedAt = "updated_at"

    val joinedColAccountNum = "account_number"
    val joinedColCurrencyCode = "currency"
  }

}
