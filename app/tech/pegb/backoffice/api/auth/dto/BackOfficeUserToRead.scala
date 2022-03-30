package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.JsValue

case class BackOfficeUserToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_name", required = true) userName: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "email", required = true) email: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "phone_number", required = false) phoneNumber: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "first_name", required = true) firstName: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "middle_name", required = false) middleName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "last_name", required = true) lastName: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "description", required = false) description: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "home_page", required = false) homePage: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "active_language", required = false) activeLanguage: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "last_login_timestamp", required = false) lastLoginTimestamp: Option[Long],
    @JsonProperty(required = true)@ApiModelProperty(name = "custom_data", required = false) customData: Option[JsValue],
    @JsonProperty(required = true)@ApiModelProperty(name = "role", required = true) role: RoleToRead,
    @JsonProperty(required = true)@ApiModelProperty(name = "business_unit", required = true) businessUnit: BusinessUnitToRead,
    @JsonProperty(required = true)@ApiModelProperty(name = "permissions", required = true) permissions: Seq[PermissionToRead],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_by", required = false) updatedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime],
    //Provided for temporary backwards compatibility for front-end
    @JsonProperty(required = true)@ApiModelProperty(name = "created_time", required = true) createdTime: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_time", required = false) updatedTime: Option[ZonedDateTime])
