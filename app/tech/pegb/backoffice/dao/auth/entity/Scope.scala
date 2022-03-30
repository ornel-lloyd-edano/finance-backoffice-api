package tech.pegb.backoffice.dao.auth.entity

import java.time.LocalDateTime

case class Scope(
    id: String,
    parentId: Option[String],
    name: String,
    description: Option[String] = None,
    isActive: Int,
    createdBy: Option[String],
    createdAt: Option[LocalDateTime],
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])
