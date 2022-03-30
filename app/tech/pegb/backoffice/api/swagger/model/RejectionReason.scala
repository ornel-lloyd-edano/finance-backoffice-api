package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "RejectionReason")
case class RejectionReason(
    @ApiModelProperty(name = "reason", example = "document is fake", required = true) reason: String,
    @ApiModelProperty(name = "updated_at") lastUpdatedAt: Option[ZonedDateTime])
