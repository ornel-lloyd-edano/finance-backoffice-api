package tech.pegb.backoffice.api.reportsv2.dto

import java.time.ZonedDateTime
import java.util.UUID

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{JsArray, Json, JsonConfiguration}

case class ReportDefinitionToRead(
    id: UUID,
    name: String,
    title: String,
    description: String,
    columns: JsArray,
    parameters: JsArray = Json.arr(),
    joins: JsArray = Json.arr(),
    grouping: JsArray = Json.arr(),
    ordering: JsArray = Json.arr(),
    paginated: Boolean,
    sql: String,
    createdAt: ZonedDateTime,
    createdBy: String,
    updatedAt: Option[ZonedDateTime],
    updatedBy: Option[String])

object ReportDefinitionToRead {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val f = Json.format[ReportDefinitionToRead]
}
