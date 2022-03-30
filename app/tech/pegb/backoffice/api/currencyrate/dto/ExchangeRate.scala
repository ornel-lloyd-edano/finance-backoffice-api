package tech.pegb.backoffice.api.currencyrate.dto

import java.util.UUID

case class ExchangeRate(
    id: UUID,
    rate: BigDecimal)
