package tech.pegb.backoffice.api.reportsv2.dto

import java.time.{LocalDate}

case class CashFlowReport(
    date: String,
    institution: String,
    currency: String,
    openingBalance: BigDecimal,
    bankTransfer: BigDecimal,
    cashIn: BigDecimal,
    cashOut: BigDecimal,
    transactions: BigDecimal,
    closingBalance: BigDecimal) {

}
