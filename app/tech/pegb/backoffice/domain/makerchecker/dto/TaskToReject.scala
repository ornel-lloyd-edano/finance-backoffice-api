package tech.pegb.backoffice.domain.makerchecker.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.makerchecker.model.RoleLevel

case class TaskToReject(
    id: UUID,
    rejectionReason: String,
    rejectedBy: String,
    rejectedAt: LocalDateTime,
    checkerLevel: RoleLevel,
    checkerBusinessUnit: String) {
}
