package tech.pegb.backoffice.dao.report.dto

import java.time.LocalDateTime

case class ReportDefinitionToInsert(
    name: String,
    title: String,
    description: String,
    columns: Option[String] = None,
    parameters: Option[String] = None,
    joins: Option[String] = None,
    grouping: Option[String] = None,
    ordering: Option[String] = None,
    paginated: Boolean,
    sql: String,
    createdBy: String,
    createdAt: LocalDateTime)
