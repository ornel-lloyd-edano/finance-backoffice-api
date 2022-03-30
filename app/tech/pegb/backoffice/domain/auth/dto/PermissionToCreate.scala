package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime
import java.util.UUID

case class PermissionToCreate(
    permissionKey: PermissionKey,
    revoke: Option[Boolean],
    scopeId: UUID,
    createdAt: LocalDateTime,
    createdBy: String)

