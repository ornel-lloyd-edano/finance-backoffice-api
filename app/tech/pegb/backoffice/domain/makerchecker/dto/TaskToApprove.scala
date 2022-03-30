package tech.pegb.backoffice.domain.makerchecker.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.makerchecker.model.RoleLevel

case class TaskToApprove(
    id: UUID,
    maybeReason: Option[String], //possible justification why not rejected
    approvedBy: String,
    approvedAt: LocalDateTime,
    checkerLevel: RoleLevel,
    checkerBusinessUnit: String) {

}
