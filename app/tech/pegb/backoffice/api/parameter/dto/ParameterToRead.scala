package tech.pegb.backoffice.api.parameter.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json.{Format, Json, JsonConfiguration}
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase

trait ParameterToReadI {
  val id: UUID
  val key: String
  val value: JsonNode
  val platforms: Seq[String]
  val metadataId: String
  val explanation: Option[String]
  val createdAt: Option[ZonedDateTime]
  val createdBy: Option[String]
  val updatedAt: Option[ZonedDateTime]
  val updatedBy: Option[String]
}

case class ParameterToRead(
    @JsonProperty(required = true) id: UUID,
    @JsonProperty(required = true) key: String,
    @JsonProperty(required = true) value: JsonNode,
    @JsonProperty(required = true) platforms: Seq[String],
    @JsonProperty(required = true) metadataId: String,
    @JsonProperty(required = true) explanation: Option[String],
    @JsonProperty(required = true) createdAt: Option[ZonedDateTime],
    @JsonProperty(required = true) createdBy: Option[String],
    @JsonProperty(required = true) updatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true) updatedBy: Option[String]) extends ParameterToReadI

object ParameterToRead {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val format: Format[ParameterToRead] = Json.format[ParameterToRead]
}
