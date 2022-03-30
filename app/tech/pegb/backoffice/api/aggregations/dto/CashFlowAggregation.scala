package tech.pegb.backoffice.api.aggregations.dto

case class CashFlowAggregation(
    currency: String,
    totalBankTransfer: BigDecimal,
    totalCashIn: BigDecimal,
    totalCashOut: BigDecimal,
    totalTxnEtc: BigDecimal)
