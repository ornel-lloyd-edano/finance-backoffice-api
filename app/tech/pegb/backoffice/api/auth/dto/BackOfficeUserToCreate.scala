package tech.pegb.backoffice.api.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import play.api.libs.json.JsValue

trait BackOfficeUserToCreateT {
  val userName: String
  val email: String
  val phoneNumber: Option[String]
  val firstName: String
  val middleName: Option[String]
  val lastName: String
  val description: Option[String]
  val homePage: Option[String]
  val activeLanguage: Option[String]
  val customData: Option[JsValue]
  val roleId: String
  val businessUnitId: String
}

case class BackOfficeUserToCreate(
    @JsonProperty(required = true) userName: String,
    @JsonProperty(required = true) email: String,
    @JsonProperty(required = false) phoneNumber: Option[String],
    @JsonProperty(required = true) firstName: String,
    @JsonProperty(required = false) middleName: Option[String],
    @JsonProperty(required = true) lastName: String,
    @JsonProperty(required = false) description: Option[String],
    @JsonProperty(required = false) homePage: Option[String],
    @JsonProperty(required = false) activeLanguage: Option[String],
    @JsonProperty(required = false) customData: Option[JsValue],
    @JsonProperty(required = true) roleId: String,
    @JsonProperty(required = true) businessUnitId: String) extends BackOfficeUserToCreateT
