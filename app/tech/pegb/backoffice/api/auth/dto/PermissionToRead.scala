package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class PermissionToRead(
    @ApiModelProperty(name = "id", example = "e37015f3-b9eb-43a8-8b1a-4f20f00ecd88", required = true) id: UUID,
    @ApiModelProperty(name = "scope") scope: ScopeToRead,
    @ApiModelProperty(name = "created_by", example = "pegbuser", required = true) createdBy: String,
    @ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime],
    //Provided for temporary backwards compatibility for front-end
    @ApiModelProperty(name = "created_time", required = true) createdTime: ZonedDateTime,
    @ApiModelProperty(name = "updated_time", required = false) updatedTime: Option[ZonedDateTime])
