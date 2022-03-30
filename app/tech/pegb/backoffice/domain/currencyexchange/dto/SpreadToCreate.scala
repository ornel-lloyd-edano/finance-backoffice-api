package tech.pegb.backoffice.domain.currencyexchange.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.currencyexchange.model.Spread
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}

case class SpreadToCreate(
    currencyExchangeId: UUID,
    transactionType: TransactionType,
    channel: Option[Channel],
    institution: Option[String],
    spread: BigDecimal,
    createdBy: String,
    createdAt: LocalDateTime) {

  Spread.validate(transactionType, channel, institution, spread)
}

