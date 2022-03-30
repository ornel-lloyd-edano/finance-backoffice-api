package tech.pegb.backoffice.dao.auth.entity

import java.time.LocalDateTime

case class Permission(
    id: String,
    businessUnitId: Option[String],
    roleId: Option[String],
    userId: Option[String],
    scope: Scope,
    isActive: Int,
    createdAt: Option[LocalDateTime],
    createdBy: Option[String],
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
