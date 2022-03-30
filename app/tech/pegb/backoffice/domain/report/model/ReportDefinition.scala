package tech.pegb.backoffice.domain.report.model

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.JsArray

case class ReportDefinition(
    id: UUID,
    name: String,
    title: String,
    description: String,
    columns: Option[JsArray],
    parameters: Option[JsArray],
    joins: Option[JsArray],
    grouping: Option[JsArray],
    ordering: Option[JsArray],
    paginated: Boolean,
    sql: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
