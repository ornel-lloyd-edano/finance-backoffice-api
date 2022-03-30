package tech.pegb.backoffice.dao.limit.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.limit.entity.{LimitProfile, LimitProfileHistory}
import tech.pegb.backoffice.dao.limit.sql.LimitProfileHistorySqlDao

@ImplementedBy(classOf[LimitProfileHistorySqlDao])
trait LimitProfileHistoryDao extends Dao {
  def createHistoryRecord(
    existing: LimitProfile,
    updated: LimitProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String]): DaoResponse[Unit]

  def fetchHistoryForProfile(limitProfileId: Int): DaoResponse[List[LimitProfileHistory]]
}
