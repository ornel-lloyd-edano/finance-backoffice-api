package tech.pegb.backoffice.dao.currencyexchange.sql

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import anorm.{Row, RowParser, SQL, SqlQuery}
import com.google.inject.Inject
import javax.inject.Singleton

import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.currencyexchange.abstraction.CurrencyExchangeDao
import tech.pegb.backoffice.dao.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.dao.currencyexchange.entity.CurrencyExchange
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.{SqlDao, model}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class CurrencyExchangeSqlDao @Inject() (
    val dbApi: DBApi,
    kafkaDBSyncService: KafkaDBSyncService)
  extends CurrencyExchangeDao with SqlDao with MostRecentUpdatedAtGetter[CurrencyExchange, CurrencyExchangeCriteria] {
  import CurrencyExchangeSqlDao._

  override protected def getUpdatedAtColumn: String = s"${CurrencyExchangeSqlDao.TableAlias}.${CurrencyExchangeSqlDao.cUpdatedAt}"

  override protected def getMainSelectQuery: String = CurrencyExchangeSqlDao.qCommonSelect

  override protected def getRowToEntityParser: Row ⇒ CurrencyExchange = (arg: Row) ⇒ CurrencyExchangeSqlDao.convertRowToCurrencyExchange(arg)

  override protected def getWhereFilterFromCriteria(criteriaDto: Option[CurrencyExchangeCriteria]): String = generateWhereFilter(criteriaDto)

  override def getDailyAmount(targetCurrencyAccountId: Long, baseCurrencyAccountId: Long): DaoResponse[Option[BigDecimal]] = withConnection({ implicit connection ⇒
    val dailyAmountSql = computeDailyAmount()
      .on(
        cTargetCurrencyAccountId → targetCurrencyAccountId,
        cBaseCurrencyAccountId → baseCurrencyAccountId)

    dailyAmountSql.as(dailyAmountSql.defaultParser.singleOpt)
      .flatMap(convertRowToDailyAmount(_))
  }, s"Error while retrieving daily amount of exchanges between currencyAccountId $targetCurrencyAccountId and $baseCurrencyAccountId")

  def countTotalCurrencyExchangeByCriteria(criteria: CurrencyExchangeCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateWhereFilter(criteria.toOption)
    val countByCriteriaSql = countCurrencyExchangeByCriteria(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def getCurrencyExchangeByCriteria(
    criteria: CurrencyExchangeCriteria,
    orderBy: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[CurrencyExchange]] = withConnection({ implicit connection ⇒

    val whereFilter = generateWhereFilter(criteria.toOption)
    val currencyExchangesByCriteriaSql = findCurrencyExchangesByCriteria(whereFilter, orderBy, limit, offset)

    currencyExchangesByCriteriaSql
      .as(currencyExchangesByCriteriaSql.defaultParser.*)
      .map(convertRowToCurrencyExchange)
  }, s"Error while retrieving currencyExchanges by criteria:$criteria")

  override def findById(id: UUID): DaoResponse[CurrencyExchange] = {
    withConnectionAndFlatten({ implicit cxn ⇒
      val idCriteria = CriteriaField(cUuid, id.toString, MatchTypes.Exact)
      val filter = generateWhereFilter(CurrencyExchangeCriteria(id = Some(idCriteria)).toOption)
      findCurrencyExchangesByCriteria(filter, maybeOrderBy = Seq.empty)
        .as(fxRowParser.singleOpt)
        .toRight(entityNotFoundError(s"Currency exchange $id was not found"))
    }, s"Failed to fetch currency exchange $id")
  }

  override def findById(dbId: Int): DaoResponse[CurrencyExchange] = {
    val idCriteria = CriteriaField(cId, dbId, MatchTypes.Exact)
    val filter = generateWhereFilter(CurrencyExchangeCriteria(dbId = Some(idCriteria)).toOption)

    withTransactionAndFlatten({ implicit cxn ⇒
      findCurrencyExchangesByCriteria(filter, maybeOrderBy = Seq.empty)
        .as(fxRowParser.singleOpt)
        .toRight(entityNotFoundError(s"Currency exchange $dbId was not found"))
    }, s"Failed to fetch currency exchange by $dbId")
  }

  def findByMultipleUuid(uuids: Seq[String]): DaoResponse[Seq[CurrencyExchange]] = {
    withConnection({ implicit cxn ⇒
      findCurrencyExchangeIdsByUuid(uuids)
    }, s"Failed to fetch currency exchange by $uuids")
  }

  def updateCurrencyExchangeStatus(id: Int, status: String): DaoResponse[Boolean] = {
    withTransaction({ implicit cxn ⇒
      val queryResponse = updateQuery.on("status" → status, "id" → id)
        .executeUpdate() > 0
      if (queryResponse) {
        findById(id).foreach { currencyExchange ⇒
          kafkaDBSyncService.sendUpdate(TableName, currencyExchange)
        }
      }
      queryResponse
    }, s"Failed to perform update of currency exchange $id")
  }
}

object CurrencyExchangeSqlDao {

  private[dao] final val TableName = "currency_rates"
  private[dao] final val TableAlias = "fx"
  private[dao] final val BaseCurrencyTableAlias = "bc"
  private[dao] final val TargetCurrencyTableAlias = "tc"

  private[dao] final val AccountsTable = "accounts"
  private[dao] final val TargetCurrencyAccountsAlias = "t_a"
  private[dao] final val BaseCurrencyAccountsAlias = "b_a"

  private[dao] final val TransactionsTable = "transactions"
  private[dao] final val BaseCurrencyTransactionsAlias = "base_tx"
  private[dao] final val TargetCurrencyTransactionsAlias = "target_tx"

  private[dao] final val cId = "id"
  private[dao] final val cUuid = "uuid"
  private[dao] final val cCurrencyId = "currency_id"
  private[dao] final val cBaseCurrencyId = "base_currency_id"
  private[dao] final val cRate = "rate"
  private[dao] final val cProviderId = "provider_id"
  private[dao] final val cStatus = "status"
  private[dao] final val cUpdatedBy = "updated_by"
  private[dao] final val cUpdatedAt = "updated_at"

  private[dao] final val cName = "name"
  private[dao] final val cProviderName = "provider_name"
  private[dao] final val cBaseCurrencyName = "base_currency"
  private[dao] final val cTargetCurrencyName = "currency_code"

  private[dao] final val cUserId = "user_id"
  private[dao] final val cBalance = "balance"
  private[dao] final val cMainType = "main_type"
  private[dao] final val cTargetCurrencyAccountId = "target_currency_account_id"
  private[dao] final val cTargetCurrencyAccountUuid = "target_currency_account_uuid"
  private[dao] final val cBaseCurrencyAccountId = "base_currency_account_id"
  private[dao] final val cBaseCurrencyAccountUuid = "base_currency_account_uuid"

  private[dao] final val cAmount = "amount"
  private[dao] final val cPrimaryAccountId = "primary_account_id"
  private[dao] final val cType = "type"
  private[dao] final val cCreatedAt = "created_at"

  private[dao] final val vLiability = "liability"
  private[dao] final val vCurrencyExchange = "currency_exchange"

  private def convertRowToCount(row: Row): Int = row[Int]("n")
  private def convertRowToDailyAmount(row: Row): Option[BigDecimal] = row[Option[BigDecimal]]("daily_amount")

  lazy val qCommonJoin: String =
    s"""
       |FROM $TableName $TableAlias
       |
       |JOIN ${CurrencySqlDao.TableName} $BaseCurrencyTableAlias
       |ON $TableAlias.$cBaseCurrencyId = $BaseCurrencyTableAlias.$cId
       |
       |JOIN ${CurrencySqlDao.TableName} $TargetCurrencyTableAlias
       |ON $TableAlias.$cCurrencyId = $TargetCurrencyTableAlias.$cId
       |
       |JOIN ${ProviderSqlDao.TableName} ${ProviderSqlDao.TableAlias}
       |ON $TableAlias.$cProviderId = ${ProviderSqlDao.TableAlias}.${Provider.cId}
       |
       |JOIN $AccountsTable $TargetCurrencyAccountsAlias
       |ON ${ProviderSqlDao.TableAlias}.$cUserId = $TargetCurrencyAccountsAlias.$cUserId
       |AND $TargetCurrencyAccountsAlias.$cMainType = '$vLiability'
       |AND $TableAlias.$cCurrencyId = $TargetCurrencyAccountsAlias.$cCurrencyId
       |
       |JOIN $AccountsTable $BaseCurrencyAccountsAlias
       |ON ${ProviderSqlDao.TableAlias}.$cUserId = $BaseCurrencyAccountsAlias.$cUserId
       |AND $BaseCurrencyAccountsAlias.$cMainType = '$vLiability'
       |AND $TableAlias.$cBaseCurrencyId = $BaseCurrencyAccountsAlias.$cCurrencyId
     """.stripMargin

  lazy val qCommonSelect: String =
    s"""
       |SELECT $TableAlias.*,
       |${ProviderSqlDao.TableAlias}.${Provider.cName} as $cProviderName,
       |$BaseCurrencyTableAlias.${CurrencySqlDao.cName} as $cBaseCurrencyName,
       |$TargetCurrencyTableAlias.${CurrencySqlDao.cName} as $cTargetCurrencyName,
       |$TargetCurrencyAccountsAlias.$cBalance,
       |$TargetCurrencyAccountsAlias.$cId as $cTargetCurrencyAccountId,
       |$TargetCurrencyAccountsAlias.$cUuid as $cTargetCurrencyAccountUuid,
       |$BaseCurrencyAccountsAlias.$cId as $cBaseCurrencyAccountId,
       |$BaseCurrencyAccountsAlias.$cUuid as $cBaseCurrencyAccountUuid
       |$qCommonJoin
     """.stripMargin

  lazy val qDailyAmount: String =
    s"""
       |SELECT SUM($TargetCurrencyTransactionsAlias.$cAmount) as daily_amount
       |FROM $TransactionsTable $TargetCurrencyTransactionsAlias
       |INNER JOIN $TransactionsTable $BaseCurrencyTransactionsAlias
       |ON $TargetCurrencyTransactionsAlias.$cId = $BaseCurrencyTransactionsAlias.$cId
       |AND $TargetCurrencyTransactionsAlias.$cPrimaryAccountId = {$cTargetCurrencyAccountId}
       |AND $BaseCurrencyTransactionsAlias.$cPrimaryAccountId = {$cBaseCurrencyAccountId}
       |AND $TargetCurrencyTransactionsAlias.$cType = '$vCurrencyExchange'
       |AND $BaseCurrencyTransactionsAlias.$cType = '$vCurrencyExchange'
       |WHERE date($TargetCurrencyTransactionsAlias.$cCreatedAt) = date(curdate());
     """.stripMargin

  private def computeDailyAmount(): SqlQuery = {
    SQL(qDailyAmount)
  }

  private def countCurrencyExchangeByCriteria(filters: String): SqlQuery = {
    SQL(s"SELECT COUNT(*) as n $qCommonJoin $filters".stripMargin)
  }

  private def findCurrencyExchangesByCriteria(
    filters: String,
    maybeOrderBy: Seq[Ordering],
    maybeLimit: Option[Int] = None,
    maybeOffset: Option[Int] = None): SqlQuery = {

    val ordering =
      if (maybeOrderBy.isEmpty) ""
      else maybeOrderBy.map(o ⇒ s"${o.field} ${o.order}").mkString("ORDER BY ", ", ", " ")

    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)
    SQL(s"$qCommonSelect $filters $ordering $pagination".stripMargin)
  }

  private def findCurrencyExchangeIdsByUuid(uuids: Seq[String])(implicit connection: Connection) = {
    val allUuids = uuids.map(d ⇒ s"'$d'").mkString("(", ", ", ")")

    val query = SQL(s"$qCommonSelect where $TableAlias.$cUuid in $allUuids".stripMargin)

    query.as(query.defaultParser.*).map { row ⇒
      convertRowToCurrencyExchange(row)
    }

  }

  private def updateQuery: SqlQuery = SQL(s"UPDATE $TableName SET status = {status} WHERE id = {id}")

  private def convertRowToCurrencyExchange(row: Row) = {
    CurrencyExchange(
      id = row[Long](cId),
      uuid = row[String](cUuid),
      currencyId = row[Long](cCurrencyId),
      currencyCode = row[String](cTargetCurrencyName),
      baseCurrencyId = row[Long](cBaseCurrencyId),
      baseCurrency = row[String](cBaseCurrencyName),
      rate = row[BigDecimal](cRate),
      providerId = row[Int](cProviderId),
      provider = row[String](cProviderName),
      targetCurrencyAccountId = row[Long](cTargetCurrencyAccountId),
      targetCurrencyAccountUuid = row[String](cTargetCurrencyAccountUuid),
      baseCurrencyAccountId = row[Long](cBaseCurrencyAccountId),
      baseCurrencyAccountUuid = row[String](cBaseCurrencyAccountUuid),
      balance = row[BigDecimal](cBalance),
      status = row[String](cStatus),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy))
  }

  private val fxRowParser: RowParser[CurrencyExchange] = row ⇒ {
    Try(convertRowToCurrencyExchange(row))
      .fold(exc ⇒ anorm.Error(anorm.SqlRequestError(exc)), anorm.Success(_))
  }

  private def generateWhereFilter(mayBeCriteria: Option[CurrencyExchangeCriteria]): String = {
    import SqlDao._
    val defaultCriteria = s"$TargetCurrencyTableAlias.${CurrencySqlDao.cIsActive} = '1'"
    mayBeCriteria.map { criteria ⇒

      val userUuidFilter = criteria.id.map { cf ⇒
        queryConditionClause(cf.value, cUuid, Some(TableAlias), cf.operator == MatchTypes.Partial)
      }

      //Since it's int, doesn't really support Partial
      val dbIdFilter = criteria.dbId.map { cf ⇒
        queryConditionClause(cf.value, cId, Some(TableAlias), cf.operator == MatchTypes.Partial)
      }

      val currencyCodeFilter = criteria.currencyCode.map { cf ⇒
        queryConditionClause(cf.value, CurrencySqlDao.cName, Some(TargetCurrencyTableAlias), cf.operator == MatchTypes.Partial)
      }

      val baseCurrencyFilter = criteria.baseCurrency.map { cf ⇒
        queryConditionClause(cf.value, CurrencySqlDao.cName, Some(BaseCurrencyTableAlias), cf.operator == MatchTypes.Partial)
      }

      val providerFilter = criteria.provider.map { cf ⇒
        queryConditionClause(cf.value, cName, Some(ProviderSqlDao.TableAlias), cf.operator == MatchTypes.Partial)
      }

      val statusFilter = criteria.status.map(queryConditionClause(_, cStatus, Some(TableAlias)))

      val filters = Seq(userUuidFilter, dbIdFilter, currencyCodeFilter, baseCurrencyFilter, providerFilter, statusFilter)
        .flatten.mkString(" AND ")

      if (filters.nonEmpty) s"WHERE $filters AND $defaultCriteria"
      else s"WHERE $defaultCriteria"
    }.getOrElse(s"WHERE $defaultCriteria")
  }
}
