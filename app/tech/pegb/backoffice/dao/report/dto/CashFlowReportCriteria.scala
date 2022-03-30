package tech.pegb.backoffice.dao.report.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class CashFlowReportCriteria(
    currencies: Option[CriteriaField[Seq[String]]] = None,
    providers: Option[CriteriaField[Seq[String]]] = None,
    createdAtFrom: Option[CriteriaField[LocalDateTime]] = None,
    createdAtTo: Option[CriteriaField[LocalDateTime]] = None,
    userType: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None,
    primaryAccNumber: Option[CriteriaField[String]] = None) {

}
