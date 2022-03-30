package tech.pegb.backoffice.dao.report.dto

case class CashFlowTotals(
    totalBankTransfers: BigDecimal,
    totalCashins: BigDecimal,
    totalCashouts: BigDecimal,
    totalTransactions: BigDecimal) {

}
