package tech.pegb.backoffice.dao.currencyexchange.dto

import java.time.LocalDateTime
import java.util.UUID

case class SpreadToInsert(
    currencyExchangeId: UUID,
    transactionType: String,
    channel: Option[String],
    institution: Option[String],
    spread: BigDecimal,
    createdAt: LocalDateTime,
    createdBy: String)
