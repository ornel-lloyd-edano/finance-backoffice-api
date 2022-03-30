package tech.pegb.backoffice.mapping.domain.api.recon

import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.api.recon.dto.{InternReconResultToRead, InternReconSummaryToRead}
import tech.pegb.backoffice.domain.reconciliation.dto.{InternReconResultDto, InternReconSummaryDto}

object Implicits {

  implicit class InternReconSummaryDomainToApiAdapter(val arg: InternReconSummaryDto) extends AnyVal {
    def asApi: InternReconSummaryToRead = {
      InternReconSummaryToRead(
        id = arg.id,
        accountNumber = arg.accountNumber,
        accountType = arg.accountType,
        accountMainType = arg.accountMainType,
        currency = arg.currency,
        userId = arg.userUuid,
        userFullName = arg.userFullName,
        customerName = arg.anyCustomerName,
        date = arg.reconDate.toZonedDateTimeUTC,
        totalValue = arg.endOfDayBalance,
        difference = arg.difference,
        totalTxn = arg.transactionTotalAmount,
        txnCount = arg.transactionTotalCount,
        incidents = arg.problematicInternReconResultsCount,
        status = arg.reconStatus,
        comments = arg.comments,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

  implicit class InternReconResultDomainToApiAdapter(val arg: InternReconResultDto) extends AnyVal {
    def asApi: InternReconResultToRead = {
      InternReconResultToRead(
        incidentId = arg.id,
        reconId = arg.internReconSummaryResultId,
        reconDate = arg.reconDate.toZonedDateTimeUTC,
        accountNumber = arg.accountNumber,
        currency = arg.currency,
        txnId = arg.currentTxnId.toString,
        txnSequence = arg.currentTxnSequence.toString,
        txnDirection = arg.currentTxnDirection,
        txnDate = arg.currentTxnTimestamp.toZonedDateTimeUTC,
        txnAmount = arg.currentTxnAmount,
        balanceBefore = arg.previousTxnPreviousBalance,
        balanceAfter = arg.currentTxnPreviousBalance)
    }
  }
}
