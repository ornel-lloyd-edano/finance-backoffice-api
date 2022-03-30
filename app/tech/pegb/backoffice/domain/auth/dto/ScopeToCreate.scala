package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime
import java.util.UUID

case class ScopeToCreate(
    name: String,
    parentId: Option[UUID],
    description: Option[String] = None,
    createdAt: LocalDateTime,
    createdBy: String)

