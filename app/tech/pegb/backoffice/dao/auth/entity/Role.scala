package tech.pegb.backoffice.dao.auth.entity

import java.time.LocalDateTime
import java.util.UUID

case class Role(id: UUID, name: String, level: Int,
    isActive: Int,
    createdBy: Option[String],
    createdAt: Option[LocalDateTime],
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}

object Role {
  val empty = Role(id = UUID.randomUUID(), name = "", level = 1, isActive = 1,
    createdBy = Some(""), createdAt = Some(LocalDateTime.now), updatedBy = None, updatedAt = None)
}
