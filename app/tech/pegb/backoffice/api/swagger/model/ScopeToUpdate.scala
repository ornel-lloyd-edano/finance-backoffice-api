package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.api.auth.dto.ScopeToUpdateT

case class ScopeToUpdate(
    @ApiModelProperty(name = "description", example = "Access to dashboard creation", required = true) description: Option[String] = None,
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime]) extends ScopeToUpdateT

