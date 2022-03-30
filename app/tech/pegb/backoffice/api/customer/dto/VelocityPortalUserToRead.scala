package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class VelocityPortalUserToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: UUID,
    @ApiModelProperty(name = "name", required = true, example = "George") name: String,
    @ApiModelProperty(name = "middle_name", required = true, example = "Otieno") middleName: Option[String],
    @ApiModelProperty(name = "surname", required = true, example = "Ogalo") surname: String,
    @ApiModelProperty(name = "full_name", required = true, example = "George Otieno Ogalo") fullName: String,
    @ApiModelProperty(name = "msisdn", required = true, example = "+254231845") msisdn: String,
    @ApiModelProperty(name = "email", required = true, example = "theboss@costacoffe.com") email: String,
    @ApiModelProperty(name = "username", required = true, example = "george.ogalo") username: String,
    @ApiModelProperty(name = "role", required = true, example = "admin") role: String,
    @ApiModelProperty(name = "status", required = true, example = "active") status: String,
    @ApiModelProperty(name = "last_login_at", required = true) lastLoginAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "created_by", example = "pegbuser", required = true) createdBy: String,
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])

