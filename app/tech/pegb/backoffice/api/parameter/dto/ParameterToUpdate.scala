package tech.pegb.backoffice.api.parameter.dto

import java.time.ZonedDateTime

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{JsValue, Json, JsonConfiguration}

case class ParameterToUpdate(
    value: JsValue,
    explanation: Option[String],
    metadataId: Option[String],
    platforms: Option[Seq[String]],
    updatedAt: Option[ZonedDateTime]) // TODO @JsonProperty("updated_at") is not compatible with play json

object ParameterToUpdate {
  private implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val parameterToUpdateFormat = Json.format[ParameterToUpdate]
}
