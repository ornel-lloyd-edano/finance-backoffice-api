package tech.pegb.backoffice.api.businessuserapplication.dto

import io.swagger.annotations.ApiModelProperty

case class BusinessUserApplicationContact(
    @ApiModelProperty(name = "contact_type", required = true) contactType: String,
    @ApiModelProperty(name = "name", required = true) name: String,
    @ApiModelProperty(name = "middle_name", required = false) middleName: Option[String],
    @ApiModelProperty(name = "surname", required = true) surname: String,
    @ApiModelProperty(name = "phone_number", required = false) phoneNumber: Option[String],
    @ApiModelProperty(name = "email", required = false) email: Option[String],
    @ApiModelProperty(name = "id_type", required = false) idType: Option[String],
    @ApiModelProperty(name = "is_velocity_user", required = true) isVelocityUser: Boolean,
    @ApiModelProperty(name = "velocity_level", required = false) velocityLevel: Option[String],
    @ApiModelProperty(name = "is_default_contact", required = false) isDefaultContact: Option[Boolean])
