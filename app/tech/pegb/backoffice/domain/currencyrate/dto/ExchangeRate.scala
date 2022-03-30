package tech.pegb.backoffice.domain.currencyrate.dto

import java.util.UUID

case class ExchangeRate(
    id: UUID,
    rate: BigDecimal)

