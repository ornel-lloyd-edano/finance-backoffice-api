package tech.pegb.backoffice.dao.transaction.sql

import anorm._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionGroupDao
import tech.pegb.backoffice.dao.transaction.dto.{TransactionCriteria, TransactionGroup, TransactionGroupings}

import scala.util.Try

@Singleton
class TransactionGroupSqlDao @Inject() (val dbApi: DBApi) extends TransactionGroupDao with SqlDao {

  def getTransactionGroups(criteria: TransactionCriteria, grouping: TransactionGroupings): DaoResponse[Seq[TransactionGroup]] =
    withConnection(
      { implicit conn ⇒

        val columnsToSelect = Seq(
          if (grouping.primaryAccountId) Some(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId}") else None,
          if (grouping.transactionType) Some(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cType}") else None,
          if (grouping.channel) Some(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cChannel}") else None,
          if (grouping.status) Some(s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cStatus}") else None,
          if (grouping.currencyCode) Some(s"${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName}") else None,
          if (grouping.provider) Some(s"${ProviderSqlDao.TableAlias}.${Provider.cName}") else None).flatten

        if (columnsToSelect.isEmpty) throw new IllegalStateException("Must have at least 1 grouping")

        val wherePredicate = TransactionSqlDao.where(Some(criteria))

        val rawSql = s"""
         |SELECT ${columnsToSelect.mkString(", ")} FROM ${TransactionSqlDao.TableName} ${TransactionSqlDao.TableAlias}
         |
         |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
         |ON ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cCurrencyId} = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
         |
         |LEFT JOIN ${ProviderSqlDao.TableName} ${ProviderSqlDao.TableAlias}
         |ON ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cProviderId} = ${ProviderSqlDao.TableAlias}.${Provider.cId}
         |
         |$wherePredicate
         |GROUP BY ${columnsToSelect.mkString(", ")}
       """.stripMargin

        logger.info(s"getTransactionGroups query = $rawSql")

        SQL(rawSql).executeQuery().as(rowParser.*)
      },
      s"Error in TransactionGroupSqlDao.getTransactionGroups",
      {
        case err: IllegalStateException ⇒
          logger.error("error encountered in [getTransactionGroups]", err)
          genericDbError("error encountered while getting transaction group")
        case err: Exception ⇒
          logger.error(s"Unexpected error in TransactionGroupSqlDao.getTransactionGroups", err)
          genericDbError("Unexpected error")
      })

  private val rowParser: RowParser[TransactionGroup] = (row: Row) ⇒ Try {
    TransactionGroup(
      accountId = Try(row[Option[String]](s"${TransactionSqlDao.TableName}.${TransactionSqlDao.cPrimaryAccountId}")).getOrElse(None),
      transactionType = Try(row[Option[String]](s"${TransactionSqlDao.TableName}.${TransactionSqlDao.cType}")).getOrElse(None),
      channel = Try(row[Option[String]](s"${TransactionSqlDao.TableName}.${TransactionSqlDao.cChannel}")).getOrElse(None),
      status = Try(row[Option[String]](s"${TransactionSqlDao.TableName}.${TransactionSqlDao.cStatus}")).getOrElse(None),
      currencyCode = Try(row[Option[String]](s"${CurrencySqlDao.TableName}.${CurrencySqlDao.cName}")).getOrElse(None),
      provider = Try(row[Option[String]](s"${ProviderSqlDao.TableName}.${Provider.cName}")).getOrElse(None))
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(exc)),
    anorm.Success(_))

}
