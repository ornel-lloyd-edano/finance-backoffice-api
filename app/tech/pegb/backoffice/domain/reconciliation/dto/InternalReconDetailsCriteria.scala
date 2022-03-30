package tech.pegb.backoffice.domain.reconciliation.dto

import java.time.LocalDate
import java.util.Currency

case class InternalReconDetailsCriteria(
    maybeReconSummaryId: Option[String] = None,
    maybeCurrency: Option[Currency] = None,
    maybeStartReconDate: Option[LocalDate] = None,
    maybeEndReconDate: Option[LocalDate] = None,
    maybeAccountNumber: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) {

}

object InternalReconDetailsCriteria {
  val empty = InternalReconDetailsCriteria()
}
