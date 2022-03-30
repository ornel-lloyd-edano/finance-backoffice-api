package tech.pegb.backoffice.domain.report.dto

import java.time.LocalDateTime

import play.api.libs.json.JsArray

case class ReportDefinitionToUpdate(
    title: String,
    description: String,
    columns: Option[JsArray] = None,
    parameters: Option[JsArray] = None,
    joins: Option[JsArray] = None,
    grouping: Option[JsArray] = None,
    ordering: Option[JsArray] = None,
    paginated: Boolean,
    sql: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime] = None)
