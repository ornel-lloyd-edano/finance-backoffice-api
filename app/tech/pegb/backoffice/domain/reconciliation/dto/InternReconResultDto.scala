package tech.pegb.backoffice.domain.reconciliation.dto

import java.time.{LocalDate, LocalDateTime}

case class InternReconResultDto(
    id: String,
    internReconSummaryResultId: String,
    reconDate: LocalDate,
    accountId: String,
    accountNumber: String,
    currency: String,

    currentTxnId: Long,
    currentTxnSequence: Int,
    currentTxnDirection: String,
    currentTxnTimestamp: LocalDateTime,
    currentTxnAmount: BigDecimal,
    currentTxnPreviousBalance: Option[BigDecimal],

    previousTxnId: Option[Long] = None,
    previousTxnSequence: Option[Int] = None,
    previousTxnDirection: Option[String] = None,
    previousTxnTimestamp: Option[LocalDateTime] = None,
    previousTxnAmount: Option[BigDecimal] = None,
    previousTxnPreviousBalance: Option[BigDecimal] = None,

    reconStatus: String)
