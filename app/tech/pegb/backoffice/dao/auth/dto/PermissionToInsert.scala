package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

case class PermissionToInsert(
    businessUnitId: Option[String],
    roleId: Option[String],
    userId: Option[String],
    canWrite: Option[Int],
    isActive: Option[Int],
    scopeId: String,
    createdAt: LocalDateTime,
    createdBy: String)
