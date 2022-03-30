package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty
import tech.pegb.backoffice.domain.auth.dto.PermissionKey

case class PermissionToUpdate(
    permissionKey: Option[PermissionKey],
    @ApiModelProperty(name = "scope_id", required = true) scopeId: Option[UUID],
    @ApiModelProperty(name = "updated_at", required = true) lastUpdatedAt: Option[ZonedDateTime])
