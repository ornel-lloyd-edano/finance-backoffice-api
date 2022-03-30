package tech.pegb.backoffice.api.aggregations.dto

case class TransactionTypeTotals(
    transactionType: String,
    turnover: BigDecimal,
    grossRevenue: BigDecimal,
    thirdPartyFees: BigDecimal)

