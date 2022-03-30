package tech.pegb.backoffice.dao.report.sql

import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao

trait CashFlowReportMetadata {
  val cDate = "txn_date"
  val cProvider = "provider_name"
  val cAccNum = "account_number"
  val cCurrency = "currency_name"
  val cOpeningBal = "opening_balance"
  val cClosingBal = "closing_balance"
  val cBankTrans = "bank_transfer"
  val cCashins = "cash_ins"
  val cCashouts = "cash_outs"
  val cOtherTxns = "etc_transactions"
  val cDirection = TransactionSqlDao.cDirection
  val cMainType = TransactionSqlDao.cPrimaryAccountMainType
  val amount = TransactionSqlDao.cAmount
  val cPrimaryAccountNumber = TransactionSqlDao.cPrimaryAccountNumber

  //TODO should be moved in their respective Dao (ex. TransactionSqlDaoMetadata, AccountSqlDaoMetadata etc)
  val txAmt = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}"
  val txCurrId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}"
  val txType = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType}"
  val txCreatedAt = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCreatedAt}"
  val txPrimaryAccId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}"
  val txProvider = s"${ProviderSqlDao.TableAlias}.${Provider.cName}"
  val txTxnProviderId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}"

  val uniqueId = TransactionSqlDao.cUniqueId
  val primaryAccPrevBal = TransactionSqlDao.cPrimaryAccountPrevBal
  val primaryAccId = TransactionSqlDao.cPrimaryAccountId
  val createdAt = TransactionSqlDao.cCreatedAt

  val accNumber = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cNumber}"
  val accId = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}"
  val accUsrId = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cUserId}"

  val currName = s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName}"
}
