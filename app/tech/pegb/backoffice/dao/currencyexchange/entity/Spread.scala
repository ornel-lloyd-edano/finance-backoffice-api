package tech.pegb.backoffice.dao.currencyexchange.entity

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.Json

case class Spread(
    id: Int,
    uuid: UUID,
    currencyExchangeId: Int,
    currencyExchangeUuid: UUID,
    transactionType: String,
    channel: Option[String],
    recipientInstitution: Option[String],
    spread: BigDecimal,
    deletedAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])

object Spread {
  implicit val f = Json.format[Spread]

  val empty = new Spread(id = 1, uuid = UUID.randomUUID(), currencyExchangeId = 1,
    currencyExchangeUuid = UUID.randomUUID(), transactionType = "currency_exchange", channel = None,
    recipientInstitution = None, spread = BigDecimal(0),
    deletedAt = None, createdBy = "user", createdAt = LocalDateTime.now, updatedBy = None, updatedAt = None)
}
