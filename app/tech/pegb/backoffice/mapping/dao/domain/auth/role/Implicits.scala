package tech.pegb.backoffice.mapping.dao.domain.auth.role

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.auth.entity.Role
import tech.pegb.backoffice.domain.auth.model.{Role ⇒ RoleDomain}

import scala.util.Try

object Implicits {

  implicit class RoleAdapter(val role: Role) extends AnyVal {

    def asDomain = Try(RoleDomain(
      id = role.id,
      name = role.name,
      level = role.level,
      createdAt = role.createdAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
      createdBy = role.createdBy.getOrElse("UNKNOWN"),
      updatedAt = role.updatedAt,
      updatedBy = role.updatedBy))
  }
}
