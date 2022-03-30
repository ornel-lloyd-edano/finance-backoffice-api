package tech.pegb.backoffice.domain.auth.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.util.Implicits._

case class Role(id: UUID, name: String, level: Int,
    createdBy: String, createdAt: LocalDateTime,
    updatedBy: Option[String], updatedAt: Option[LocalDateTime]) {
  import Role._

  assert(isValidName(name), "role name cannot be empty")
  assert(isValidLevel(level), s"role level must be between $minRoleLevel to $maxRoleLevel inclusive")
}

object Role {
  val minRoleLevel = 0
  val maxRoleLevel = 4

  val empty = Role(id = UUID.randomUUID(), name = "test", level = 1,
    createdBy = "pegbuser", createdAt = LocalDateTime.now(),
    updatedBy = None, updatedAt = None)

  def isValidName(name: String): Boolean = name.hasSomething

  def isValidLevel(level: Int): Boolean = (level >= minRoleLevel) && (level <= maxRoleLevel)
}
