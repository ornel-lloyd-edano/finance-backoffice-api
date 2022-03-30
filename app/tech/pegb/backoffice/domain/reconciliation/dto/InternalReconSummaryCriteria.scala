package tech.pegb.backoffice.domain.reconciliation.dto

import java.time.LocalDate

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.util.HasPartialMatch

case class InternalReconSummaryCriteria(
    maybeId: Option[String] = None,
    maybeStartReconDate: Option[LocalDate] = None,
    maybeEndReconDate: Option[LocalDate] = None,
    maybeAccountNumber: Option[String] = None,
    maybeAccountType: Option[String] = None,
    maybeUserId: Option[String] = None,
    mayBeAnyCustomerName: Option[NameAttribute] = None,
    maybeStatus: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch {

}

object InternalReconSummaryCriteria {
  val empty = InternalReconSummaryCriteria()
}
