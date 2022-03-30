package tech.pegb.backoffice.dao.report.dto

import java.time.LocalDate

case class CashFlowReportRow(
    date: LocalDate,
    provider: String,
    accountNum: String,
    currency: String,
    openingBalance: BigDecimal,
    closingBalance: BigDecimal,
    bankTransfer: BigDecimal,
    cashins: BigDecimal,
    cashouts: BigDecimal,
    transactions: BigDecimal) {

}
