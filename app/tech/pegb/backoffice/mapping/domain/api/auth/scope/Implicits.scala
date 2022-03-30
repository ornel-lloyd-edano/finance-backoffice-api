package tech.pegb.backoffice.mapping.domain.api.auth.scope

import tech.pegb.backoffice.api.auth.dto.ScopeToRead
import tech.pegb.backoffice.domain.auth.model.Scope
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class ScopeToReadAdapter(val arg: Scope) extends AnyVal {
    def asApi: ScopeToRead = {
      ScopeToRead(
        id = arg.id,
        parentId = arg.parentId,
        name = arg.name,
        description = arg.description,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),

        createdTime = arg.createdAt.toZonedDateTimeUTC,
        updatedTime = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

}
