package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.JsValue
import tech.pegb.backoffice.api.auth.dto.BackOfficeUserToUpdateT

case class BackOfficeUserToUpdate(
    @ApiModelProperty(name = "email", required = false) email: Option[String],
    @ApiModelProperty(name = "phone_number", required = false) phoneNumber: Option[String],
    @ApiModelProperty(name = "first_name", required = false) firstName: Option[String],
    @ApiModelProperty(name = "middle_name", required = false) middleName: Option[String],
    @ApiModelProperty(name = "last_name", required = false) lastName: Option[String],
    @ApiModelProperty(name = "description", required = false) description: Option[String],
    @ApiModelProperty(name = "home_page", required = false) homePage: Option[String],
    @ApiModelProperty(name = "active_language", required = false) activeLanguage: Option[String],
    @ApiModelProperty(name = "custom_data", required = false) customData: Option[JsValue],
    @ApiModelProperty(name = "role_id", required = false) roleId: Option[String],
    @ApiModelProperty(name = "business_unit_id", required = false) businessUnit: Option[String],
    @ApiModelProperty(name = "updated_at", required = false) lastUpdatedAt: Option[ZonedDateTime]) extends BackOfficeUserToUpdateT
