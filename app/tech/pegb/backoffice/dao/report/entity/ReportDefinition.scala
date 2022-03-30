package tech.pegb.backoffice.dao.report.entity

import java.time.LocalDateTime

case class ReportDefinition(
    id: String,
    name: String,
    title: String,
    description: String,
    columns: Option[String],
    parameters: Option[String],
    joins: Option[String],
    grouping: Option[String],
    ordering: Option[String],
    paginated: Boolean,
    sql: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])
