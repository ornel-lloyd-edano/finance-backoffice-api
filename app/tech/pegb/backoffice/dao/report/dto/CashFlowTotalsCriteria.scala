package tech.pegb.backoffice.dao.report.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class CashFlowTotalsCriteria(
    currency: Option[CriteriaField[String]] = None,
    providers: Option[CriteriaField[Seq[String]]] = None,
    createdAtFrom: Option[CriteriaField[LocalDateTime]] = None,
    createdAtTo: Option[CriteriaField[LocalDateTime]] = None) {

}
