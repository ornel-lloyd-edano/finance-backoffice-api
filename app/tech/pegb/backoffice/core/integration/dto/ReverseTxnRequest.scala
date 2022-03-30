package tech.pegb.backoffice.core.integration.dto

import java.time.LocalDateTime

import play.api.libs.json.{Json, JsonConfiguration}
import play.api.libs.json.JsonNaming.SnakeCase

case class ReverseTxnRequest(
    id: String,
    isFeeReversed: Boolean,
    reason: String,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime])

object ReverseTxnRequest {
  implicit val config = JsonConfiguration(SnakeCase)

  implicit val revTxnRespFormat = Json.format[ReverseTxnRequest]
}
