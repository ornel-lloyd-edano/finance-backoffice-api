package tech.pegb.backoffice.domain.report.model

import tech.pegb.backoffice.domain.report.abstraction
import tech.pegb.backoffice.domain.report.dto.CashFlowReportLine

case class CashFlowReport(reportLines: Seq[CashFlowReportLine]) extends abstraction.CashFlowReport {

  def getTotalBankTransfer(currencyCode: String): BigDecimal = ???

  def getTotalCashin(currencyCode: String): BigDecimal = ???

  def getTotalCashout(currencyCode: String): BigDecimal = ???

  def getTotalTransactions(currencyCode: String): BigDecimal = ???

}
