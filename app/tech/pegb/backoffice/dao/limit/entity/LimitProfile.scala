package tech.pegb.backoffice.dao.limit.entity

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.Json

case class LimitProfile(
    id: Int,
    uuid: UUID,
    limitType: String,
    userType: String,
    tier: String,
    subscription: String,
    transactionType: Option[String],
    channel: Option[String],
    provider: Option[String],
    instrument: Option[String],
    interval: Option[String],
    maxIntervalAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minAmount: Option[BigDecimal],
    maxCount: Option[Int],
    currencyCode: String,
    deletedAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])

object LimitProfile {
  implicit val f = Json.format[LimitProfile]
}
