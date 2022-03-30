package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

case class ScopeToInsert(
    name: String,
    parentId: Option[String],
    description: Option[String] = None,
    isActive: Int,
    createdBy: String,
    createdAt: LocalDateTime)
