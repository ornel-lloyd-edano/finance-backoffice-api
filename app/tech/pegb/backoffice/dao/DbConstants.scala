package tech.pegb.backoffice.dao

import tech.pegb.backoffice.dao.account.sql.{AccountSqlDao, AccountTypesSqlDao}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao

//TODO refactor this and put to respective Dao companion objects
object DbConstants {

  val txnType = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType}"
  val txnDirection = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cDirection}"
  val txnAmt = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cAmount}"
  val txnCost = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCostRate}"
  val txnEffective = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cEffectiveRate}"
  val txnCurrencyId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId}"
  val txnPrimaryAccountId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}"
  val txnProvider = s"${ProviderSqlDao.TableAlias}.${Provider.cName}"
  val txnProviderAlias = "provider_name"
  val txnProviderId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId}"

  val txnCreatedAt = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCreatedAt}"
  val txnDashboardRevenue = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cDashboardRevenue}"
  val txnPrimaryAccountUserId = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountUserId}"

  val accountId = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cId}"
  val accountMainType = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cMainType}"
  val accountBalance = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cBalance}"
  val accountCurrencyId = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cCurrencyId}"
  val accountsTypeIdFKeyRef = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cAccountTypeId}"
  val accountNumber = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cNumber}"
  val accountNumberAlias = "account_number"

  val accountsTypeId = s"${AccountTypesSqlDao.TableAlias}.${AccountTypesSqlDao.cId}"

  val currencyId = s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}"
  val currencyName = s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName}"

  val accountType = s"${AccountTypesSqlDao.TableAlias}.${AccountTypesSqlDao.cAccountType}"

  val userId = s"${UserSqlDao.TableAlias}.${UserSqlDao.id}"
  val userType = s"${UserSqlDao.TableAlias}.${UserSqlDao.typeName}"

  val txnPrimaryAccountNumber = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountNumber}"

}

