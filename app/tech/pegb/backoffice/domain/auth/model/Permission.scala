package tech.pegb.backoffice.domain.auth.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.auth.dto.PermissionKey

case class Permission(
    id: UUID,
    permissionKey: PermissionKey,
    scope: Scope,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
