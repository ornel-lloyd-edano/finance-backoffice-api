package tech.pegb.backoffice.mapping.domain.dao.reconciliation

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.reconciliation.dto.{InternalReconDetailsCriteria ⇒ InternalReconDetailsCriteriaDao, InternalReconSummaryCriteria ⇒ InternalReconSummaryCriteriaDao}
import tech.pegb.backoffice.dao.reconciliation.model
import tech.pegb.backoffice.dao.reconciliation.model.{InternReconResult ⇒ InternReconResultDao}
import tech.pegb.backoffice.dao.reconciliation.postgresql.ReconciliationDao.ReconDetailsTable
import tech.pegb.backoffice.domain.reconciliation.dto.{InternReconDailySummaryResultResolve, InternalReconDetailsCriteria, InternalReconSummaryCriteria}
import tech.pegb.backoffice.domain.reconciliation.model.{InternReconResult}

object Implicits {

  implicit class InternalReconSummaryCriteriaAdapter(val arg: InternalReconSummaryCriteria) extends AnyVal {
    def asDao: InternalReconSummaryCriteriaDao = {
      val startLocalDateTime = arg.maybeStartReconDate.map(_.atTime(0, 0, 0))
      val endLocalDateTime = arg.maybeEndReconDate.map(_.atTime(23, 59, 59))

      val dataRange = (startLocalDateTime, endLocalDateTime) match {
        case (Some(startDate), Some(endDate)) ⇒
          Some(CriteriaField[(LocalDateTime, LocalDateTime)](
            "recon_date",
            (startDate, endDate), MatchTypes.InclusiveBetween))
        case (Some(createdAtFrom), None) ⇒
          Some(CriteriaField[(LocalDateTime, LocalDateTime)]("recon_date", (createdAtFrom, createdAtFrom), MatchTypes.InclusiveBetween))
        case (None, Some(createdAtTo)) ⇒
          Some(CriteriaField[(LocalDateTime, LocalDateTime)]("recon_date", (createdAtTo, createdAtTo), MatchTypes.InclusiveBetween))
        case _ ⇒ None
      }

      InternalReconSummaryCriteriaDao(

        maybeId = arg.maybeId.map(id ⇒ CriteriaField("", id,
          if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
        maybeDateRange = dataRange,
        maybeAccountNumber = arg.maybeAccountNumber.map(accountNumber ⇒ CriteriaField("", accountNumber,
          if (arg.partialMatchFields.contains("account_number")) MatchTypes.Partial else MatchTypes.Exact)),
        maybeAccountType = arg.maybeAccountType.map(accountType ⇒ CriteriaField("", accountType,
          if (arg.partialMatchFields.contains("account_type")) MatchTypes.Partial else MatchTypes.Exact)),
        maybeUserUuid = arg.maybeUserId.map(uuid ⇒ CriteriaField("", uuid,
          if (arg.partialMatchFields.contains("user_id")) MatchTypes.Partial else MatchTypes.Exact)),
        maybeAnyCustomerName =
          arg.mayBeAnyCustomerName.map(name ⇒ CriteriaField("", name.underlying,
            if (arg.partialMatchFields.contains("any_customer_name")) MatchTypes.Partial else MatchTypes.Exact)),
        maybeStatus = arg.maybeStatus.map(status ⇒ CriteriaField("", status,
          if (arg.partialMatchFields.contains("status")) MatchTypes.Partial else MatchTypes.Exact)))
    }
  }

  implicit class InternalReconResultCriteriaAdapter(val arg: InternalReconDetailsCriteria) extends AnyVal {
    def asDao: InternalReconDetailsCriteriaDao = {
      val startLocalDateTime = arg.maybeStartReconDate.map(_.atTime(0, 0, 0))
      val endLocalDateTime = arg.maybeEndReconDate.map(_.atTime(23, 59, 59))

      val dataRange = (startLocalDateTime, endLocalDateTime) match {
        case (Some(startDate), Some(endDate)) ⇒
          Some(CriteriaField[(LocalDateTime, LocalDateTime)](
            "recon_date",
            (startDate, endDate), MatchTypes.InclusiveBetween))
        case (Some(createdAtFrom), None) ⇒
          Some(CriteriaField[(LocalDateTime, LocalDateTime)]("recon_date", (createdAtFrom, createdAtFrom), MatchTypes.InclusiveBetween))
        case (None, Some(createdAtTo)) ⇒
          Some(CriteriaField[(LocalDateTime, LocalDateTime)]("recon_date", (createdAtTo, createdAtTo), MatchTypes.InclusiveBetween))
        case _ ⇒ None
      }

      InternalReconDetailsCriteriaDao(
        maybeReconSummaryId =
          arg.maybeReconSummaryId.map(summaryId ⇒ CriteriaField(ReconDetailsTable.cSummaryId, summaryId,
            if (arg.partialMatchFields.contains("recon_id")) MatchTypes.Partial else MatchTypes.Exact)),
        mayBeDateRange = dataRange,
        maybeAccountNumber =
          arg.maybeAccountNumber.map(accNum ⇒ CriteriaField(ReconDetailsTable.cAccountNumber, accNum,
            if (arg.partialMatchFields.contains("account_number")) MatchTypes.Partial else MatchTypes.Exact)),
        maybeCurrency =
          arg.maybeCurrency.map(currency ⇒ CriteriaField(ReconDetailsTable.cCurrency, currency.getCurrencyCode)))
    }
  }

  implicit class InterReconResultAdapter(val internReconResult: InternReconResult) extends AnyVal {
    def asDao(summaryId: String, status: String): InternReconResultDao = {
      InternReconResultDao(
        id = s"${internReconResult.reconDate.toString}" +
          s":${internReconResult.account.id}" +
          s":${internReconResult.currentTransaction.id}" +
          s":${internReconResult.currentTransaction.sequence}",
        internReconSummaryResultId = summaryId,
        reconDate = internReconResult.reconDate,
        accountId = internReconResult.account.id,
        accountNumber = internReconResult.account.accountNumber,
        currency = internReconResult.currentTransaction.currency.getCurrencyCode,
        currentTransactionId = internReconResult.currentTransaction.uniqueId,
        currentTxnSequence = internReconResult.currentTransaction.sequence,
        currentTxnDirection = internReconResult.currentTransaction.direction.underlying,
        currentTxnTimestamp = internReconResult.currentTransaction.dateTime,
        currentTxnAmount = internReconResult.currentTransaction.amount,
        previousTxnAmount = internReconResult.prevTransaction.map(_.amount),
        currentTxnPreviousBalance = internReconResult.currentPreviousBalance,
        previousTxnPreviousBalance = internReconResult.prevTransaction.flatMap(_.previousBalance),
        reconStatus = status)
    }
  }

  implicit class InternalReconDailySummaryResultToUpdateAdapter(val domain: InternReconDailySummaryResultResolve) extends AnyVal {
    def asDao(status: String): model.InternReconDailySummaryResultToUpdate = {
      model.InternReconDailySummaryResultToUpdate(
        status = status.some,
        comments = domain.comments.some,
        updatedBy = domain.updatedBy,
        updatedAt = domain.updatedAt,
        lastUpdatedAt = domain.lastUpdatedAt)
    }
  }

}
