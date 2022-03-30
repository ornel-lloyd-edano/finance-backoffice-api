package tech.pegb.backoffice.mapping.dao.domain.auth.permission

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.auth.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import tech.pegb.backoffice.dao.auth.entity.Permission
import tech.pegb.backoffice.domain.auth.dto.PermissionKey
import tech.pegb.backoffice.domain.auth.model
import tech.pegb.backoffice.mapping.dao.domain.auth.scope.Implicits._

import scala.util.Try

object Implicits {

  implicit class PermissionAdapter(val arg: Permission) extends AnyVal {

    def asDomain: Try[model.Permission] = {
      for {
        scopeDomain ← arg.scope.asDomain
        permissionDomain ← Try {
          val permissionKey: PermissionKey =
            (arg.userId, arg.roleId, arg.businessUnitId) match {
              case (Some(userId), None, None) ⇒
                UserPermissionKey(userId = UUID.fromString(userId))
              case (_, Some(roleId), Some(businessUnitId)) ⇒
                BusinessUnitAndRolePermissionKey(buId = UUID.fromString(businessUnitId), roleId = UUID.fromString(roleId))
              case _ ⇒ throw new IllegalArgumentException("PermissionKey can be composed by a defined userID or combination of businessUnitId and roleId")
            }

          model.Permission(
            id = UUID.fromString(arg.id),
            permissionKey = permissionKey,
            scope = scopeDomain,
            createdBy = arg.createdBy.getOrElse("UNKNOWN"),
            createdAt = arg.createdAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
            updatedBy = arg.updatedBy,
            updatedAt = arg.updatedAt)
        }
      } yield {
        permissionDomain
      }
    }
  }
}
