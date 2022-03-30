package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class BusinessUnitToRead(

    @ApiModelProperty(name = "id", example = "e37015f3-b9eb-43a8-8b1a-4f20f00ecd88", required = true) id: UUID,
    @ApiModelProperty(name = "name", example = "", required = true) name: String,
    @ApiModelProperty(name = "created_by", example = "pegb_user", required = true) createdBy: String,
    @ApiModelProperty(name = "updated_by", example = "pegb_user", required = false) updatedBy: Option[String],
    @ApiModelProperty(name = "created_at", example = "", required = true) createdAt: ZonedDateTime,
    @ApiModelProperty(name = "updated_at", example = "", required = false) updatedAt: Option[ZonedDateTime],
    //Provided for temporary backwards compatibility for front-end
    @ApiModelProperty(name = "created_time", required = true) createdTime: ZonedDateTime,
    @ApiModelProperty(name = "updated_time", required = false) updatedTime: Option[ZonedDateTime])
