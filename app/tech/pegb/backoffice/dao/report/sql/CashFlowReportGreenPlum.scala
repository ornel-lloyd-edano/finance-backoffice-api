package tech.pegb.backoffice.dao.report.sql

import anorm.SQL
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.{PostgresDao, RowParsingException}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.report.abstraction.CashFlowReportDao
import tech.pegb.backoffice.dao.report.dto.{CashFlowReportCriteria, CashFlowReportRow, CashFlowTotals, CashFlowTotalsCriteria}
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.api.aggregations.controllers.Constants
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao

@Singleton
class CashFlowReportGreenPlum @Inject() (
    appConfig: AppConfig,
    val dbApi: DBApi) extends CashFlowReportDao with CashFlowReportRowParser with PostgresDao {

  override val cDate = "txn_date"
  override val primaryAccPrevBal = "previous_balance"

  import PostgresDao._ //needed by .toSql in getWhereClause

  def getCashFlowReport(criteria: CashFlowReportCriteria): DaoResponse[Seq[CashFlowReportRow]] =
    withConnectionHasDefaultSchema(
      { implicit conn ⇒

        val where = getWhereClauseFromCashFlowReportCriteria(criteria.toOption)

        val rawSQL =
          s"""
           |SELECT
           |
           |  DATE($txCreatedAt) as ${this.cDate},
           |  $txProvider as ${this.cProvider},
           |  $accNumber as ${this.cAccNum},
           |  $currName as ${this.cCurrency},
           |
           |  (SELECT tx1.$primaryAccPrevBal
           |	FROM ${TransactionSqlDao.TableName} tx1
           |    WHERE DATE(tx1.$createdAt) = DATE($txCreatedAt)
           |    AND tx1.$primaryAccId = $txPrimaryAccId
           |	ORDER BY tx1.$createdAt ASC, tx1.$uniqueId ASC
           |	LIMIT 1) as ${this.cOpeningBal},
           |
           |  (SELECT tx2.$primaryAccPrevBal + (
           |  CASE
           |     WHEN tx2.$cDirection = 'debit'  AND tx2.$cMainType = 'asset' THEN tx2.$amount
           |     WHEN tx2.$cDirection = 'credit' AND tx2.$cMainType = 'asset' THEN -tx2.$amount
           |     WHEN tx2.$cDirection = 'credit' AND tx2.$cMainType = 'liability' THEN tx2.$amount
           |     WHEN tx2.$cDirection = 'debit'  AND tx2.$cMainType = 'liability' THEN -tx2.$amount
           |  ELSE 0
           |END
           |  )
           |	FROM ${TransactionSqlDao.TableName} tx2
           |    WHERE DATE(tx2.$createdAt) = DATE($txCreatedAt)
           |    AND tx2.$primaryAccId = $txPrimaryAccId
           |	ORDER BY tx2.$createdAt DESC, tx2.$uniqueId DESC
           |	LIMIT 1) as ${this.cClosingBal},
           |
           |  SUM(CASE WHEN $txType = '${Constants.TxnType.BankTransfer}' THEN $txAmt ELSE 0.0 END) as ${this.cBankTrans},
           |
           |  SUM(CASE WHEN $txType = '${Constants.TxnType.CashIn}' THEN $txAmt ELSE 0.0 END) as ${this.cCashins},
           |
           |  SUM(CASE WHEN $txType = '${Constants.TxnType.CashOut}' THEN $txAmt ELSE 0.0 END) as ${this.cCashouts},
           |
           |  SUM(CASE WHEN $txType NOT IN ('${Constants.TxnType.CashIn}', '${Constants.TxnType.CashOut}', '${Constants.TxnType.BankTransfer}') THEN $txAmt ELSE 0.0 END) as ${this.cOtherTxns}
           |
           |FROM ${TransactionSqlDao.TableName} ${TransactionSqlDao.TableAlias}
           |
           |JOIN ${AccountSqlDao.TableName} ${AccountSqlDao.TableAlias} ON ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId} = $accId
           |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias} ON $accUsrId = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
           |
           |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias} ON $txCurrId = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
           |
           |JOIN ${ProviderSqlDao.TableName} ${ProviderSqlDao.TableAlias} ON $txTxnProviderId = ${ProviderSqlDao.TableAlias}.${Provider.cId}
           |
           |$where
           |
           |GROUP BY ${this.cDate}, ${this.cProvider}, ${this.cAccNum}, ${this.cCurrency}, ${this.cOpeningBal}, ${this.cClosingBal}
           |ORDER BY ${this.cDate} DESC, ${this.cAccNum} ASC, ${this.cCurrency} ASC;
         """.stripMargin

        logger.debug("cashflow report query = " + rawSQL)
        val sql = SQL(rawSQL)
        sql.executeQuery().as(sql.defaultParser.*)
          .map(parseCashFlowReportRow(_).getOrElse(throw new RowParsingException("Unable to parse result of cash flow report query.")))

      }, appConfig.SchemaName,
      s"Unexpected error in ${this.getClass.getSimpleName}.getCashFlowReport",
      {
        case err: RowParsingException ⇒
          logger.error(s"Row parsing exception in ${this.getClass.getSimpleName}.getCashFlowReport", err)
          rowParsingError(err.getMessage)
        case err: Exception ⇒
          logger.error(s"Unhandled exception in ${this.getClass.getSimpleName}.getCashFlowReport", err)
          genericDbError(err.getMessage)
      })

  private def getWhereClauseFromCashFlowReportCriteria(criteria: Option[CashFlowReportCriteria]): String = {
    criteria.fold[String]("") { criteria ⇒
      Seq(
        criteria.currencies.map(_.toSql(tableAlias = CurrencySqlDao.TableAlias.toOption)),
        criteria.providers.map(_.toSql(tableAlias = ProviderSqlDao.TableAlias.toOption)),
        criteria.createdAtFrom.map(_.toSql(tableAlias = TransactionSqlDao.TableAlias.toOption)),
        criteria.createdAtTo.map(_.toSql(tableAlias = TransactionSqlDao.TableAlias.toOption)),
        criteria.userType.map(_.toSql(tableAlias = UserSqlDao.TableAlias.toOption)),
        criteria.provider.map(_.toSql(tableAlias = ProviderSqlDao.TableAlias.toOption)),
        criteria.primaryAccNumber.map(_.toSql(tableAlias = TransactionSqlDao.TableAlias.toOption)))
        .flatten.toSql
    }
  }

}
