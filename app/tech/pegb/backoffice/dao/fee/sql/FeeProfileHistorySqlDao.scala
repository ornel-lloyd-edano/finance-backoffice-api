package tech.pegb.backoffice.dao.fee.sql

import java.sql.Connection
import java.time.LocalDateTime

import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.fee.abstraction.FeeProfileHistoryDao
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileHistory}
import tech.pegb.backoffice.util.AppConfig

class FeeProfileHistorySqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig)
  extends FeeProfileHistoryDao with SqlDao {

  override def createHistoryRecord(
    existing: FeeProfile,
    updated: FeeProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String]): DaoResponse[Unit] = ???

  override def fetchHistoryForProfile(limitProfileId: Int): DaoResponse[List[FeeProfileHistory]] = ???

  private[sql] def internalCreateHistoryRecord(
    existing: FeeProfile,
    updated: FeeProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String])(
    implicit
    cxn: Connection): Unit = {
    // convertLimitProfileToSqlQuery(existing, updated, updatedBy, updatedAt, reason).executeInsert()
  }

}
