package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime
import java.util.UUID

case class RoleToCreate(
    id: UUID,
    name: String,
    isActive: Int,
    level: Int,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: String,
    updatedAt: LocalDateTime)
