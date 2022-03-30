package tech.pegb.backoffice.domain.report.dto

case class CashFlowTotals(
    currency: String,
    totalBankTransfer: BigDecimal,
    totalCashin: BigDecimal,
    totalCashout: BigDecimal,
    totalTransactions: BigDecimal) {

}

