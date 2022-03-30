package tech.pegb.backoffice.domain.currencyexchange.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.currencyexchange.model.Spread
import tech.pegb.backoffice.util.LastUpdatedAt

case class SpreadUpdateDto(
    id: UUID,
    currencyExchangeId: UUID,
    spread: BigDecimal,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends LastUpdatedAt {

  Spread.validateSpreadValue(spread)
}
