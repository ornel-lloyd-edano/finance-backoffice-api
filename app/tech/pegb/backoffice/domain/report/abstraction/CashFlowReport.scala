package tech.pegb.backoffice.domain.report.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.report.dto.CashFlowReportLine
import tech.pegb.backoffice.domain.report.model.{CashFlowReport ⇒ CashFlowReportImpl}

@ImplementedBy(classOf[CashFlowReportImpl])
trait CashFlowReport {
  def reportLines: Seq[CashFlowReportLine]

  def size: Int = reportLines.size

  def getTotalBankTransfer(currencyCode: String): BigDecimal

  def getTotalCashin(currencyCode: String): BigDecimal

  def getTotalCashout(currencyCode: String): BigDecimal

  def getTotalTransactions(currencyCode: String): BigDecimal
}
