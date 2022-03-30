package tech.pegb.backoffice.mapping.api.domain.auth.scope

import java.time.ZonedDateTime

import tech.pegb.backoffice.api.auth.dto.{ScopeToCreate, ScopeToUpdate}
import tech.pegb.backoffice.domain.auth.dto
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class ScopeToCreateAdapter(val arg: ScopeToCreate) extends AnyVal {
    def asDomain(createdAt: ZonedDateTime, createdBy: String): dto.ScopeToCreate = {
      dto.ScopeToCreate(
        name = arg.name.sanitize,
        parentId = arg.parentId,
        description = arg.description.map(_.sanitize),
        createdAt = createdAt.toLocalDateTimeUTC,
        createdBy = createdBy)
    }
  }

  implicit class ScopeToUpdateAdapter(val arg: ScopeToUpdate) extends AnyVal {
    def asDomain(updatedAt: ZonedDateTime, updatedBy: String): dto.ScopeToUpdate = {
      dto.ScopeToUpdate(
        description = arg.description.map(_.sanitize),
        updatedBy = updatedBy,
        updatedAt = updatedAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }
}
