package tech.pegb.backoffice.domain.report.dto

import tech.pegb.backoffice.util.HasPartialMatch

case class ReportDefinitionCriteria(
    id: Option[String] = None,
    name: Option[String] = None,
    title: Option[String] = None,
    description: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
