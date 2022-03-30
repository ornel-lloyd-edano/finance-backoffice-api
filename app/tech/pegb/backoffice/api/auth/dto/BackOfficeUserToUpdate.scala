package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import play.api.libs.json.JsValue

trait BackOfficeUserToUpdateT {
  val email: Option[String]
  val phoneNumber: Option[String]
  val firstName: Option[String]
  val middleName: Option[String]
  val lastName: Option[String]
  val description: Option[String]
  val homePage: Option[String]
  val activeLanguage: Option[String]
  val customData: Option[JsValue]
  val roleId: Option[String]
  val businessUnit: Option[String]
  val lastUpdatedAt: Option[ZonedDateTime]
}

case class BackOfficeUserToUpdate(
    @JsonProperty(required = false) email: Option[String],
    @JsonProperty(required = false) phoneNumber: Option[String],
    @JsonProperty(required = false) firstName: Option[String],
    @JsonProperty(required = false) middleName: Option[String],
    @JsonProperty(required = false) lastName: Option[String],
    @JsonProperty(required = false) description: Option[String],
    @JsonProperty(required = false) homePage: Option[String],
    @JsonProperty(required = false) activeLanguage: Option[String],
    @JsonProperty(required = false) customData: Option[JsValue],
    @JsonProperty(required = false) roleId: Option[String],
    @JsonProperty(required = false) businessUnit: Option[String],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) extends BackOfficeUserToUpdateT
