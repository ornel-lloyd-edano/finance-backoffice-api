package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.JsValue
import tech.pegb.backoffice.api.auth.dto.BackOfficeUserToCreateT

case class BackOfficeUserToCreate(
    @ApiModelProperty(name = "user_name", required = false) userName: String,
    @ApiModelProperty(name = "email", required = false) email: String,
    @ApiModelProperty(name = "phone_number", required = false) phoneNumber: Option[String],
    @ApiModelProperty(name = "first_name", required = false) firstName: String,
    @ApiModelProperty(name = "middle_name", required = false) middleName: Option[String],
    @ApiModelProperty(name = "last_name", required = false) lastName: String,
    @ApiModelProperty(name = "description", required = false) description: Option[String],
    @ApiModelProperty(name = "home_page", required = false) homePage: Option[String],
    @ApiModelProperty(name = "active_language", required = false) activeLanguage: Option[String],
    @ApiModelProperty(name = "custom_data", required = false) customData: Option[JsValue],
    @ApiModelProperty(name = "role_id", required = false) roleId: String,
    @ApiModelProperty(name = "business_unit_id", required = false) businessUnitId: String) extends BackOfficeUserToCreateT
