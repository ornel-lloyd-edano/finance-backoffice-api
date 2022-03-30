package tech.pegb.backoffice.domain.report.dto

import java.time.LocalDate

case class CashFlowReportLine(
    date: LocalDate,
    provider: String,
    account: String,
    openingBalance: BigDecimal,
    closingBalance: BigDecimal,
    bankTransfer: BigDecimal,
    cashin: BigDecimal,
    cashout: BigDecimal,
    transactions: BigDecimal,
    currency: String)
