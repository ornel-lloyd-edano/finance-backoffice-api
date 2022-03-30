package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "WalletApplicationToReject")
case class WalletApplicationToReject(
    @ApiModelProperty(name = "reason", example = "missing document", required = true) reason: String,
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime])
