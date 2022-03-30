package tech.pegb.backoffice.domain.txn.model

import java.time.LocalDateTime

final case class Transaction(
    id: Long,
    sequence: Long,
    primaryAccountId: Option[Long],
    secondaryAccountId: Option[Long],
    receiverPhone: Option[String],
    direction: Option[String],
    `type`: Option[String],
    amount: Option[BigDecimal],
    currency: Option[String],
    exchangeRate: Option[BigDecimal],
    channel: Option[String],
    otherParty: Option[String],
    instrument: Option[String],
    instrumentId: Option[String],
    latitude: Option[BigDecimal],
    longitude: Option[BigDecimal],
    explanation: Option[String],
    status: Option[String],
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime])

object Transaction {
  def empty(id: Long) = Transaction(
    id = id,
    sequence = 0L,
    primaryAccountId = None,
    secondaryAccountId = None,
    receiverPhone = None,
    direction = None,
    `type` = None,
    amount = None,
    currency = None,
    exchangeRate = None,
    channel = None,
    otherParty = None,
    instrument = None,
    instrumentId = None,
    latitude = None,
    longitude = None,
    explanation = None,
    status = None,
    createdAt = None,
    updatedAt = None)
}
