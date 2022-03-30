package tech.pegb.backoffice.core.integration.dto

import java.time.LocalDateTime

import play.api.libs.json.{Json, JsonConfiguration}
import play.api.libs.json.JsonNaming.SnakeCase

case class TxnCancellationRequest(id: String, updatedBy: String, reason: String, lastUpdatedAt: Option[LocalDateTime])

object TxnCancellationRequest {
  implicit val config = JsonConfiguration(SnakeCase)

  implicit val cancelTxnFormat = Json.format[TxnCancellationRequest]
}
