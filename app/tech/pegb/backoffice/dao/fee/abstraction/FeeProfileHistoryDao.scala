package tech.pegb.backoffice.dao.fee.abstraction

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileHistory}

trait FeeProfileHistoryDao extends Dao {
  def createHistoryRecord(
    existing: FeeProfile,
    updated: FeeProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String]): DaoResponse[Unit]

  def fetchHistoryForProfile(limitProfileId: Int): DaoResponse[List[FeeProfileHistory]]
}
