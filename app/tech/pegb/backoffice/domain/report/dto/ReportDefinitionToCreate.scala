package tech.pegb.backoffice.domain.report.dto

import java.time.LocalDateTime

import play.api.libs.json.JsArray

case class ReportDefinitionToCreate(
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
    createdBy: String,
    createdAt: LocalDateTime)
