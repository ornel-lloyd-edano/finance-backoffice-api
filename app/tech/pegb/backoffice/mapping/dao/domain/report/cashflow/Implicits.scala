package tech.pegb.backoffice.mapping.dao.domain.report.cashflow

import tech.pegb.backoffice.dao.report.dto.{CashFlowReportRow, CashFlowTotals}
import tech.pegb.backoffice.domain.report.dto.{CashFlowReportLine, CashFlowTotals ⇒ DaoCashFlowTotals}
import tech.pegb.backoffice.domain.report.model.CashFlowReport
import tech.pegb.backoffice.domain.report.abstraction

object Implicits {

  implicit class CashFlowReportRowAdapter(val arg: Iterable[CashFlowReportRow]) extends AnyVal {
    def asDomain: abstraction.CashFlowReport = CashFlowReport(
      reportLines = arg.map(c ⇒ CashFlowReportLine(
        date = c.date,
        provider = c.provider,
        account = c.accountNum,
        openingBalance = c.openingBalance,
        closingBalance = c.closingBalance,
        bankTransfer = c.bankTransfer,
        cashin = c.cashins,
        cashout = c.cashouts,
        transactions = c.transactions,
        currency = c.currency)).toSeq)
  }

  implicit class CashFlowTotalsAdapter(val arg: CashFlowTotals) extends AnyVal {
    def asDomain(currency: String) = DaoCashFlowTotals(
      currency = currency,
      totalBankTransfer = arg.totalBankTransfers,
      totalCashin = arg.totalCashins,
      totalCashout = arg.totalCashouts,
      totalTransactions = arg.totalTransactions)
  }
}
