package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class VelocityPortalResetPinRequest(
    @ApiModelProperty(name = "reason", required = true) reason: String,
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime])
