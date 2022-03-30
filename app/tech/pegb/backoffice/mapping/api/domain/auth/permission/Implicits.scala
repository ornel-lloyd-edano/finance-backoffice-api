package tech.pegb.backoffice.mapping.api.domain.auth.permission

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.auth.dto.{PermissionKey, PermissionToCreate, PermissionToUpdate}
import tech.pegb.backoffice.domain.auth.dto
import tech.pegb.backoffice.domain.auth.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object Implicits {

  implicit class PermissionCriteriaAdapter(val arg: (Option[UUID], Option[UUID], Option[UUID], Option[UUID], Option[UUID], Set[String])) extends AnyVal {
    def asDomain(): dto.PermissionCriteria = {
      dto.PermissionCriteria(
        id = arg._1.map(x ⇒ UUIDLike(x.toString)),
        businessId = arg._2.map(x ⇒ UUIDLike(x.toString)),
        roleId = arg._3.map(x ⇒ UUIDLike(x.toString)),
        userId = arg._4.map(x ⇒ UUIDLike(x.toString)),
        scopeId = arg._5.map(x ⇒ UUIDLike(x.toString)),
        partialMatchFields = arg._6)
    }
  }

  implicit class PermissionToCreateAdapter(val arg: PermissionToCreate) extends AnyVal {
    def asDomain(createdAt: ZonedDateTime, createdBy: String): Try[dto.PermissionToCreate] = Try {
      dto.PermissionToCreate(
        permissionKey = arg.permissionKey.asDomain(),
        revoke = arg.revoke,
        scopeId = arg.scopeId,
        createdAt = createdAt.toLocalDateTimeUTC,
        createdBy = createdBy)
    }
  }

  implicit class PermissionKeyAdapter(val arg: PermissionKey) extends AnyVal {
    def asDomain(): dto.PermissionKey =
      (arg.userId, arg.buId, arg.roleId) match {
        case (Some(id), _, _) ⇒ UserPermissionKey(id)
        case (None, Some(buId), Some(roleId)) ⇒ BusinessUnitAndRolePermissionKey(buId, roleId)
        case _ ⇒ throw new IllegalArgumentException("Permission key should contain either user_id or (bu_id and role_id)")
      }
  }

  implicit class PermissionToUpdateAdapter(val arg: PermissionToUpdate) extends AnyVal {
    def asDomain(updatedAt: ZonedDateTime, updatedBy: String): Try[dto.PermissionToUpdate] = Try {
      dto.PermissionToUpdate(
        permissionKey = arg.permissionKey.map(_.asDomain()),
        scopeId = arg.scopeId,
        updatedBy = updatedBy,
        updatedAt = updatedAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }
}
