package tech.pegb.backoffice.domain.transaction.model

import java.time.LocalDateTime

case class SettlementFxHistory(
    fxProvider: String,
    fromCurrencyId: Int,
    fromCurrency: String,
    fromIcon: String,
    toCurrencyId: Int,
    toCurrency: String,
    toIcon: String,
    fxRate: BigDecimal,
    createdAt: LocalDateTime)

