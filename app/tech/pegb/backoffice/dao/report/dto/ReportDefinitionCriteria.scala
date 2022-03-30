package tech.pegb.backoffice.dao.report.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class ReportDefinitionCriteria(
    id: Option[CriteriaField[String]] = None,
    name: Option[String] = None,
    title: Option[String] = None,
    description: Option[CriteriaField[String]] = None)
