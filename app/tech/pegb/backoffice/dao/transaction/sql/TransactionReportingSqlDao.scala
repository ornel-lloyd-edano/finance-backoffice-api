package tech.pegb.backoffice.dao.postgresql

import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.PostgresDao
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.util.AppConfig

class TransactionReportingSqlDao @Inject() (
    config: AppConfig,
    override val dbApi: DBApi) extends TransactionSqlDao(dbApi = dbApi, config = config) with PostgresDao {

  override def db = dbApi.database("reports")

}
