package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class RoleToRead(
    @ApiModelProperty(name = "id", example = "e37015f3-b9eb-43a8-8b1a-4f20f00ecd88", required = true) id: UUID,
    @ApiModelProperty(name = "name", example = "role_name", required = true) name: String,
    @ApiModelProperty(name = "level", example = "1", required = true) level: Int,
    @ApiModelProperty(name = "created_by", example = "pegb_user", required = true) createdBy: String,
    @ApiModelProperty(name = "updated_by", example = "pegb_user", required = true) updatedBy: Option[String],
    @ApiModelProperty(name = "created_at", example = "2019-10-17T08:26:09.198Z", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_at", example = "2019-10-17T08:26:09.198Z", required = true) updatedAt: Option[ZonedDateTime],
    //Provided for temporary backwards compatibility for front-end
    @ApiModelProperty(name = "created_time", required = true) createdTime: ZonedDateTime,
    @ApiModelProperty(name = "updated_time", required = false) updatedTime: Option[ZonedDateTime])
