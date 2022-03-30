package tech.pegb.backoffice.domain.currencyrate.model

import java.util.UUID

case class ExchangeRate(
    id: UUID,
    rate: BigDecimal)
