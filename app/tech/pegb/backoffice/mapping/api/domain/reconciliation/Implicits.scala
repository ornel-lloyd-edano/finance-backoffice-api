package tech.pegb.backoffice.mapping.api.domain.reconciliation

import java.time.LocalDateTime
import java.util.Currency

import tech.pegb.backoffice.api.recon.dto.InternReconDailySummaryResultResolve
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.reconciliation.dto
import tech.pegb.backoffice.domain.reconciliation.dto.{InternalReconDetailsCriteria, InternalReconSummaryCriteria}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class InternalReconSummaryCriteriaAdapter(val arg: (Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[LocalDateTime], Option[LocalDateTime], Set[String])) extends AnyVal {

    def asDomain: InternalReconSummaryCriteria = {
      InternalReconSummaryCriteria(
        maybeId = arg._1.map(_.sanitize),
        maybeAccountNumber = arg._2.map(_.sanitize),
        maybeAccountType = arg._3.map(_.sanitize),
        maybeUserId = arg._4.map(_.sanitize),
        mayBeAnyCustomerName = arg._5.map(name ⇒ NameAttribute(name.sanitize)),
        maybeStatus = arg._6.map(_.sanitize),
        maybeStartReconDate = arg._7.map(_.toLocalDate),
        maybeEndReconDate = arg._8.map(_.toLocalDate),
        partialMatchFields = arg._9)
    }
  }

  implicit class InternalReconResultCriteriaAdapter(val arg: (Option[String], Option[String], Option[String], Option[LocalDateTime], Option[LocalDateTime], Set[String])) extends AnyVal {
    def asDomain: InternalReconDetailsCriteria = {
      InternalReconDetailsCriteria(
        maybeReconSummaryId = arg._1.map(_.sanitize),
        maybeAccountNumber = arg._2.map(_.sanitize),
        maybeCurrency = arg._3.map(c ⇒ Currency.getInstance(c.sanitize)),
        maybeStartReconDate = arg._4.map(_.toLocalDate),
        maybeEndReconDate = arg._5.map(_.toLocalDate),
        partialMatchFields = arg._6)
    }
  }

  implicit class InternReconDailySummaryResultToUpdateAdapter(val arg: InternReconDailySummaryResultResolve) extends AnyVal {
    def asDomain(updatedBy: String, updatedAt: LocalDateTime): dto.InternReconDailySummaryResultResolve = {
      dto.InternReconDailySummaryResultResolve(
        comments = arg.comments,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }

}
