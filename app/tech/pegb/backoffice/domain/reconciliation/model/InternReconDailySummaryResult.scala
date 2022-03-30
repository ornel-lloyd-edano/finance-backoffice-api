package tech.pegb.backoffice.domain.reconciliation.model

import java.time.{LocalDate, LocalDateTime}
import java.util.Currency

case class InternReconDailySummaryResult(
    reconDate: LocalDate,
    accountId: String,
    accountNumber: String,
    accountType: String,
    accountMainType: AccountMainType,
    userId: Int,
    userUuid: String,
    currency: Currency,
    endOfDayBalance: BigDecimal,
    valueChange: BigDecimal,
    transactionTotalAmount: BigDecimal,
    transactionTotalCount: Int,
    problematicInternReconResults: Seq[InternReconResult],
    status: ReconciliationStatus,
    comments: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None,
    updateBy: Option[String] = None) {
  if (status == ReconciliationStatuses.NOK) {

    assert(transactionTotalCount >= problematicInternReconResults.size, "total count of transactions must be at least equal to the number of problematic internal reconciliation results")
  }
  assert(!problematicInternReconResults.exists(_.account.id != this.accountId), "all problematic internal recon results should belong to same account")
}
