package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class RoleToUpdate(
    name: Option[String] = None,
    level: Option[Int] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None)
