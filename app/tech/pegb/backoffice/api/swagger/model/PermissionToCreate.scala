package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import tech.pegb.backoffice.api.auth.dto.PermissionKey

case class PermissionToCreate(
    permissionKey: PermissionKey,
    revoke: Option[Boolean],
    scopeId: UUID)
