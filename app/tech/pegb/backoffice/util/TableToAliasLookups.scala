package tech.pegb.backoffice.util

import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.{BusinessUserSqlDao, IndividualUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao

object TableToAliasLookups {

  final val tableAliasMap = Map(
    TransactionSqlDao.TableAlias -> TransactionSqlDao.TableName,
    AccountSqlDao.TableAlias -> AccountSqlDao.TableName,
    CurrencySqlDao.TableAlias -> CurrencySqlDao.TableName,
    BusinessUserSqlDao.TableAlias -> BusinessUserSqlDao.TableName,
    IndividualUserSqlDao.TableAlias -> IndividualUserSqlDao.TableName,
    UserSqlDao.TableAlias -> UserSqlDao.TableName,
    ProviderSqlDao.TableAlias → ProviderSqlDao.TableName)

}
