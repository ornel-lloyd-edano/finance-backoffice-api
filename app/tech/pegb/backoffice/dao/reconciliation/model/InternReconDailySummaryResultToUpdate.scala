package tech.pegb.backoffice.dao.reconciliation.model

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.reconciliation.postgresql.ReconciliationDao._

case class InternReconDailySummaryResultToUpdate(
    status: Option[String] = None,
    reconDate: Option[LocalDate] = None,
    accountId: Option[String] = None,
    accountNumber: Option[String] = None,
    accountType: Option[String] = None,
    accountMainType: Option[String] = None,
    userUuid: Option[String] = None,
    currency: Option[String] = None,
    endOfDayBalance: Option[BigDecimal] = None,
    valueChange: Option[BigDecimal] = None,
    transactionTotalAmount: Option[BigDecimal] = None,
    transactionTotalCount: Option[Int] = None,
    problematicTxnCount: Option[Int] = None,
    comments: Option[String],
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  status.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cStatus → x))
  reconDate.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cReconDate → x))
  accountId.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cAccountId → x))
  accountNumber.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cAccountNumber → x))
  accountType.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cAccountType → x))
  accountMainType.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cAccountMainType → x))
  userUuid.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cUserUuid → x))
  currency.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cCurrency → x))
  endOfDayBalance.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cEndOfDayBal → x))
  valueChange.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cValueChange → x))
  transactionTotalAmount.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cTxnTotalAmt → x))
  transactionTotalCount.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cTxnTotalCnt → x))
  problematicTxnCount.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cProblemCnt → x))
  comments.foreach(x ⇒ appendForGreenPlum(ReconSummaryTable.cComments → x))

  appendForGreenPlum(ReconSummaryTable.cUpdatedBy → updatedBy)
  appendForGreenPlum(ReconSummaryTable.cUpdatedAt → updatedAt)

}
