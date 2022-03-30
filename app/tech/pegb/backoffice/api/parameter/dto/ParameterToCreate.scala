package tech.pegb.backoffice.api.parameter.dto

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{JsValue, Json, JsonConfiguration}

case class ParameterToCreate(
    key: String,
    value: JsValue,
    explanation: Option[String],
    metadataId: String,
    platforms: Seq[String])

object ParameterToCreate {
  private implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val parameterToCreateFormat = Json.format[ParameterToCreate]
}
