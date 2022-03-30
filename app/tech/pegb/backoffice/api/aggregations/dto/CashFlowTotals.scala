package tech.pegb.backoffice.api.aggregations.dto

case class CashFlowTotals(
    totalBankTransfer: BigDecimal,
    totalCashIn: BigDecimal,
    totalCashOut: BigDecimal,
    totalTransaction: BigDecimal) {

}
