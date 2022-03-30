package tech.pegb.backoffice.dao.auth.entity

import java.time.LocalDateTime

case class BusinessUnit(
    id: String,
    name: String,
    isActive: Int,
    createdBy: Option[String],
    createdAt: Option[LocalDateTime],
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])

object BusinessUnit {
  lazy val empty = BusinessUnit(
    id = "",
    name = "",
    isActive = 1,
    createdBy = Some(""),
    updatedBy = None,
    createdAt = Some(LocalDateTime.now()),
    updatedAt = None)
}
