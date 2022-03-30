package tech.pegb.backoffice.api.auth.dto

import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class PermissionToCreate(
    permissionKey: PermissionKey,
    @ApiModelProperty(name = "revoke") revoke: Option[Boolean],
    @ApiModelProperty(name = "scope_id", required = true) scopeId: UUID)

case class PermissionKey(
    buId: Option[UUID],
    roleId: Option[UUID],
    userId: Option[UUID])
