package tech.pegb.backoffice.mapping.domain.api.auth.permission

import tech.pegb.backoffice.api.auth.dto.PermissionToRead
import tech.pegb.backoffice.domain.auth.model.Permission
import tech.pegb.backoffice.mapping.domain.api.auth.scope.Implicits._
import tech.pegb.backoffice.util.Implicits._

object Implicits {
  implicit class PermissionToReadAdapter(val arg: Permission) extends AnyVal {
    def asApi: PermissionToRead = {
      PermissionToRead(
        id = arg.id,
        scope = arg.scope.asApi,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),

        createdTime = arg.createdAt.toZonedDateTimeUTC,
        updatedTime = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }
}
