package tech.pegb.backoffice.mapping.dao.domain.auth.scope

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.auth.entity.Scope
import tech.pegb.backoffice.domain.auth.model

import scala.util.Try

object Implicits {

  implicit class ScopeAdapter(val arg: Scope) extends AnyVal {
    def asDomain = Try {
      model.Scope(
        id = UUID.fromString(arg.id),
        parentId = arg.parentId.map(UUID.fromString(_)),
        name = arg.name,
        description = arg.description,
        createdBy = arg.createdBy.getOrElse("UNKNOWN"),
        createdAt = arg.createdAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt)
    }
  }
}
