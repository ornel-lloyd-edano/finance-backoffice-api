package tech.pegb.backoffice.mapping.dao.domain.reconciliation

import tech.pegb.backoffice.dao.reconciliation.model.{InternReconDailySummaryResult, InternReconResult}
import tech.pegb.backoffice.domain.reconciliation.dto.{InternReconResultDto, InternReconSummaryDto}

object Implicits {

  implicit class InternReconSummaryDtoAdapterOne(val daoModel: InternReconDailySummaryResult) extends AnyVal {
    def asDomainDto(difference: BigDecimal): InternReconSummaryDto = {
      InternReconSummaryDto(
        id = daoModel.id,
        accountId = daoModel.accountId,
        accountNumber = daoModel.accountNumber,
        accountType = daoModel.accountType,
        accountMainType = daoModel.accountMainType,
        currency = daoModel.currency,
        userUuid = daoModel.userUuid,
        userFullName = daoModel.userFullName,
        anyCustomerName = daoModel.anyCustomerName,
        reconDate = daoModel.reconDate,

        endOfDayBalance = daoModel.endOfDayBalance,
        valueChange = daoModel.valueChange,
        difference = difference,
        transactionTotalAmount = daoModel.transactionTotalAmount,
        transactionTotalCount = daoModel.transactionTotalCount,

        problematicInternReconResultsCount = daoModel.problematicTxnCount,

        reconStatus = daoModel.status,
        comments = daoModel.comments,
        updatedAt = daoModel.updatedAt)
    }
  }

  implicit class InternReconResultDtoAdapter(val daoModel: InternReconResult) extends AnyVal {
    def asDomain: InternReconResultDto = {
      InternReconResultDto(
        id = daoModel.id,
        internReconSummaryResultId = daoModel.internReconSummaryResultId,
        reconDate = daoModel.reconDate,
        accountId = daoModel.accountId,
        accountNumber = daoModel.accountNumber,
        currency = daoModel.currency,
        currentTxnId = daoModel.currentTransactionId,
        currentTxnSequence = daoModel.currentTxnSequence,
        currentTxnDirection = daoModel.currentTxnDirection,
        currentTxnTimestamp = daoModel.currentTxnTimestamp,
        currentTxnAmount = daoModel.currentTxnAmount,
        currentTxnPreviousBalance = daoModel.currentTxnPreviousBalance,

        previousTxnId = daoModel.previousTransactionId,
        previousTxnSequence = daoModel.previousTxnSequence,
        previousTxnDirection = daoModel.previousTxnDirection,
        previousTxnTimestamp = daoModel.previousTxnTimestamp,
        previousTxnAmount = daoModel.previousTxnAmount,
        previousTxnPreviousBalance = daoModel.previousTxnPreviousBalance,
        reconStatus = daoModel.reconStatus)
    }
  }

}
