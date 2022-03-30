package tech.pegb.backoffice.dao.limit.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.{RowParser, SQL, SqlRequestError}
import javax.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.limit.abstraction.LimitProfileHistoryDao
import tech.pegb.backoffice.dao.limit.entity.{LimitProfile, LimitProfileHistory}

import scala.util.Try

class LimitProfileHistorySqlDao @Inject() (
    override protected val dbApi: DBApi)
  extends LimitProfileHistoryDao with SqlDao {
  import LimitProfileHistorySqlDao._

  override def createHistoryRecord(
    existing: LimitProfile,
    updated: LimitProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String]): DaoResponse[Unit] = {
    withConnection({ implicit cxn ⇒
      internalCreateHistoryRecord(existing, updated, updatedBy, updatedAt, reason)
    }, s"Failed to create history entry for limit profile ${existing.uuid}")
  }

  override def fetchHistoryForProfile(limitProfileId: Int): DaoResponse[List[LimitProfileHistory]] = {
    withConnection({ implicit cxn ⇒
      fetchQuery.on(cLimitProfileId → limitProfileId).as(historyRowParser.*)
    }, s"Failed to fetch history entries for limit profile $limitProfileId")
  }

  private[sql] def internalCreateHistoryRecord(
    existing: LimitProfile,
    updated: LimitProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String])(
    implicit
    cxn: Connection): Unit = {
    convertLimitProfileToSqlQuery(existing, updated, updatedBy, updatedAt, reason).executeInsert()
  }
}

object LimitProfileHistorySqlDao {
  val TableName = "limit_profile_history"

  val cId = "id"
  val cLimitProfileId = "limit_profile_id"
  val cOldMaxIntervalAmount = "old_max_interval_amount"
  val cOldMaxAmount = "old_max_amount"
  val cOldMinAmount = "old_min_amount"
  val cOldMaxCount = "old_max_count"
  val cNewMaxIntervalAmount = "new_max_interval_amount"
  val cNewMaxAmount = "new_max_amount"
  val cNewMinAmount = "new_min_amount"
  val cNewMaxCount = "new_max_count"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
  val cReason = "reason"

  private val insertQuery =
    SQL(s"INSERT INTO $TableName ($cLimitProfileId," +
      s"$cOldMaxIntervalAmount, $cOldMaxAmount, $cOldMinAmount, $cOldMaxCount," +
      s"$cNewMaxIntervalAmount, $cNewMaxAmount, $cNewMinAmount, $cNewMaxCount," +
      s"$cUpdatedBy, $cUpdatedAt, $cReason) VALUES ({$cLimitProfileId}," +
      s"{$cOldMaxIntervalAmount}, {$cOldMaxAmount}, {$cOldMinAmount}, {$cOldMaxCount}," +
      s"{$cNewMaxIntervalAmount}, {$cNewMaxAmount}, {$cNewMinAmount}, {$cNewMaxCount}," +
      s"{$cUpdatedBy}, {$cUpdatedAt}, {$cReason})")

  private val fetchQuery =
    SQL(s"SELECT * FROM $TableName WHERE $cLimitProfileId = {$cLimitProfileId} ORDER BY $cId")

  private val historyRowParser: RowParser[LimitProfileHistory] = row ⇒ {
    Try {
      LimitProfileHistory(
        id = row[Int](cId),
        limitProfileId = row[Int](cLimitProfileId),
        oldMaxIntervalAmount = row[Option[BigDecimal]](cOldMaxIntervalAmount),
        oldMaxAmount = row[Option[BigDecimal]](cOldMaxAmount),
        oldMinAmount = row[Option[BigDecimal]](cOldMinAmount),
        oldMaxCount = row[Option[Int]](cOldMaxCount),
        newMaxIntervalAmount = row[Option[BigDecimal]](cNewMaxIntervalAmount),
        newMaxAmount = row[Option[BigDecimal]](cNewMaxAmount),
        newMinAmount = row[Option[BigDecimal]](cNewMinAmount),
        newMaxCount = row[Option[Int]](cNewMaxCount),
        updatedBy = row[String](cUpdatedBy),
        updatedAt = row[LocalDateTime](cUpdatedAt),
        reason = row[Option[String]](cReason))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def convertLimitProfileToSqlQuery(
    existing: LimitProfile,
    updated: LimitProfile,
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String]) = {
    insertQuery
      .on(
        cLimitProfileId → existing.id,
        cOldMaxIntervalAmount → existing.maxIntervalAmount,
        cOldMaxAmount → existing.maxAmount,
        cOldMinAmount → existing.minAmount,
        cOldMaxCount → existing.maxCount,
        cNewMaxIntervalAmount → updated.maxIntervalAmount,
        cNewMaxAmount → updated.maxAmount,
        cNewMinAmount → updated.minAmount,
        cNewMaxCount → updated.maxCount,
        cUpdatedBy → updatedBy,
        cUpdatedAt → updatedAt,
        cReason → reason)
  }
}
