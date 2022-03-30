package tech.pegb.backoffice.mapping.domain.api.auth.role

import tech.pegb.backoffice.api.auth.dto.RoleToRead
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.auth.model.Role

object Implicits {

  implicit class RoleToReadAdapter(val arg: Role) extends AnyVal {
    def asApi: RoleToRead = {

      RoleToRead(
        id = arg.id,
        name = arg.name,
        level = arg.level,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        updatedBy = arg.updatedBy,

        createdTime = arg.createdAt.toZonedDateTimeUTC,
        updatedTime = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

}
