package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class ContactToCreate(
    @ApiModelProperty(name = "contact_type", required = true, example = "business_owner") contactType: String,
    @ApiModelProperty(name = "name", required = true, example = "John") name: String,
    @ApiModelProperty(name = "middle_name", required = true, example = "Smith") middleName: Option[String],
    @ApiModelProperty(name = "surname", required = true, example = "Doe") surname: String,
    @ApiModelProperty(name = "phone_number", required = true, example = "+97188888888") phoneNumber: String,
    @ApiModelProperty(name = "email", required = true, example = "j.doe@gmail.com") email: String,
    @ApiModelProperty(name = "id_type", required = true, example = "national_id") idType: String)

