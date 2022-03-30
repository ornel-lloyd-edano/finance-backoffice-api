package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class ContactToUpdate(
    @ApiModelProperty(name = "contact_type", required = true, example = "business_owner") contactType: Option[String],
    @ApiModelProperty(name = "name", required = true, example = "John") name: Option[String],
    @ApiModelProperty(name = "middle_name", required = true, example = "Smith") middleName: Option[String],
    @ApiModelProperty(name = "surname", required = true, example = "Doe") surname: Option[String],
    @ApiModelProperty(name = "phone_number", required = true, example = "+97188888888") phoneNumber: String,
    @ApiModelProperty(name = "email", required = true, example = "j.doe@gmail.com") email: Option[String],
    @ApiModelProperty(name = "id_type", required = true, example = "national_id") idType: Option[String],
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime])
