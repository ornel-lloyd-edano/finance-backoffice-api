package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class BusinessUserApplicationContactInfoToRead(
    @ApiModelProperty(name = "id", required = true, example = "da2a3868-4bbe-4bdc-adde-8e8e61bef2df") id: String,
    @ApiModelProperty(name = "status", required = true, example = "approved") status: String,
    @ApiModelProperty(name = "contacts", required = true, example = "[]") contacts: Seq[BusinessUserApplicationContact],
    @ApiModelProperty(name = "addresses", required = true, example = "[]") addresses: Seq[BusinessUserApplicationAddress],
    @ApiModelProperty(name = "created_at", required = true, example = "2020-01-01T00:00:00Z") createdAt: ZonedDateTime,
    @ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @ApiModelProperty(name = "updated_at", required = false, example = "2020-01-01T00:00:00Z") updatedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "updated_by", required = false) updatedBy: Option[String],
    @ApiModelProperty(name = "submitted_by", required = false) submittedBy: Option[String])
