package tech.pegb.backoffice.api.makerchecker.dto

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class TaskToRead(
    @ApiModelProperty(name = "id", example = "fee518f1-7b1e-4c40-86c1-8c281a0d0811", required = true) id: String,
    @ApiModelProperty(name = "module", example = "transaction", required = true) module: String,
    @ApiModelProperty(name = "action", example = "Create manual transaction", required = true) action: String,
    @ApiModelProperty(name = "status", example = "pending", required = true) status: String,
    @ApiModelProperty(name = "reason", required = false) reason: Option[String],
    @ApiModelProperty(name = "created_at", example = "2019-12-30T00:00:00Z", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "created_by", example = "Backoffice User", required = true) createdBy: String,
    @ApiModelProperty(name = "checked_at", example = "2019-12-30T00:00:00Z", required = false) checkedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "checked_by", example = "Backoffice User", required = false) checkedBy: Option[String],
    @ApiModelProperty(name = "updated_at", example = "2019-12-30T00:00:00Z", required = false) updatedAt: Option[ZonedDateTime],
    @ApiModelProperty(name = "is_read_only", example = "true", required = false) isReadOnly: Boolean)
