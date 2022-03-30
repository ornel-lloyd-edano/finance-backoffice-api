package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class ContactToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: UUID,
    @ApiModelProperty(name = "contact_type", required = true, example = "business_owner") contactType: String,
    @ApiModelProperty(name = "name", required = true, example = "George") name: String,
    @ApiModelProperty(name = "middle_name", required = true, example = "Otieno") middleName: Option[String],
    @ApiModelProperty(name = "surname", required = true, example = "Ogalo") surname: String,
    @ApiModelProperty(name = "phone_number", required = true, example = "+254237123") phoneNumber: String,
    @ApiModelProperty(name = "email", required = true, example = "theboss@costacoffee.com") email: String,
    @ApiModelProperty(name = "id_type", required = true, example = "national_id") idType: String,
    @ApiModelProperty(name = "created_by", example = "pegbuser", required = true) createdBy: String,
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])
