package tech.pegb.backoffice.dao.currencyexchange.dto

import java.time.LocalDateTime
import java.util.UUID

final case class CurrencyExchangeUpdateDto(
    id: UUID,
    updatedBy: String,
    updatedAt: LocalDateTime,
    maybeCurrencyCode: Option[String],
    maybeCurrencyDescription: Option[String],
    maybeBaseCurrency: Option[String],
    maybeBuyCost: Option[BigDecimal],
    maybeSellCost: Option[BigDecimal],
    maybeProvider: Option[String],
    maybeBalance: Option[BigDecimal],
    maybeStatus: Option[String])
