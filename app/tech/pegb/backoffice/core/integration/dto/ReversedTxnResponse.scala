package tech.pegb.backoffice.core.integration.dto

import play.api.libs.json.{Json, JsonConfiguration}
import play.api.libs.json.JsonNaming.SnakeCase

case class ReversedTxnResponse(
    id: String,
    reversedTransactionId: Int,
    reversalTransactionId: Int,
    reason: String,
    updatedBy: String,
    status: String)

object ReversedTxnResponse {
  implicit val config = JsonConfiguration(SnakeCase)

  implicit val revTxnReqFormat = Json.format[ReversedTxnResponse]
}
