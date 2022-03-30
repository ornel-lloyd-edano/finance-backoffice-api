package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class ScopeToUpdate(
    description: Option[String] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None)

