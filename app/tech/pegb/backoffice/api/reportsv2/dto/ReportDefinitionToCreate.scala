package tech.pegb.backoffice.api.reportsv2.dto

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{JsArray, Json, JsonConfiguration, OFormat}

case class ReportDefinitionToCreate(
    name: String,
    title: String,
    description: String,
    columns: Option[JsArray] = None,
    parameters: Option[JsArray] = None,
    joins: Option[JsArray] = None,
    grouping: Option[JsArray] = None,
    ordering: Option[JsArray] = None,
    paginated: Boolean,
    sql: String)

object ReportDefinitionToCreate {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val f: OFormat[ReportDefinitionToCreate] = Json.format[ReportDefinitionToCreate]
}
