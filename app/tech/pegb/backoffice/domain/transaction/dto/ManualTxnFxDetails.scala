package tech.pegb.backoffice.domain.transaction.dto

case class ManualTxnFxDetails(
    fxProvider: String,
    fromCurrency: String,
    toCurrency: String,
    fxRate: BigDecimal)
