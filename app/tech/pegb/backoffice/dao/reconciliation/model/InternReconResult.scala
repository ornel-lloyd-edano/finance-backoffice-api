package tech.pegb.backoffice.dao.reconciliation.model

import java.time.{LocalDate, LocalDateTime}

case class InternReconResult(
    id: String,
    internReconSummaryResultId: String,
    reconDate: LocalDate,
    accountId: String,
    accountNumber: String,
    currency: String,
    currentTransactionId: Long,
    currentTxnSequence: Int,
    currentTxnDirection: String,
    currentTxnTimestamp: LocalDateTime,
    currentTxnAmount: BigDecimal,
    currentTxnPreviousBalance: Option[BigDecimal],

    previousTransactionId: Option[Long] = None,
    previousTxnSequence: Option[Int] = None,
    previousTxnDirection: Option[String] = None,
    previousTxnTimestamp: Option[LocalDateTime] = None,
    previousTxnAmount: Option[BigDecimal] = None,
    previousTxnPreviousBalance: Option[BigDecimal] = None,

    reconStatus: String)
