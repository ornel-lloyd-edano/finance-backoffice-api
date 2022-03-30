package tech.pegb.backoffice.domain.report.dto

import java.time.LocalDate

case class CashFlowTotalsCriteria(
    currency: String,
    dateFrom: Option[LocalDate] = None,
    dateTo: Option[LocalDate] = None,
    onlyForTheseProviders: Seq[String] = Nil) {

}
