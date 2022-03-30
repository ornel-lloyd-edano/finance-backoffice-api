package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime
import java.util.UUID

case class PermissionToUpdate(
    permissionKey: Option[PermissionKey] = None,
    scopeId: Option[UUID] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None)
