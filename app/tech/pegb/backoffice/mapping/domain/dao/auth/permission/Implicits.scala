package tech.pegb.backoffice.mapping.domain.dao.auth.permission

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.domain.auth.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import tech.pegb.backoffice.dao.auth.dto
import tech.pegb.backoffice.dao.auth.dto.PermissionToInsert
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.auth.dto.{PermissionCriteria, PermissionToCreate, PermissionToUpdate}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class PermissionToCreateAdapter(val arg: PermissionToCreate) extends AnyVal {
    def asDao: PermissionToInsert = {
      val (buId, roleId, userId) = arg.permissionKey match {
        case BusinessUnitAndRolePermissionKey(buId, roleId) ⇒
          (buId.toString.some, roleId.toString.some, none[String])
        case UserPermissionKey(userId) ⇒
          (none[String], none[String], userId.toString.some)
      }

      PermissionToInsert(
        businessUnitId = buId,
        roleId = roleId,
        userId = userId,
        canWrite = 1.some,
        isActive = arg.revoke.map(revoke ⇒ (!revoke).toInt).orElse(1.some),
        scopeId = arg.scopeId.toString,
        createdAt = arg.createdAt,
        createdBy = arg.createdBy)
    }

    def asReactivateDao(lastUpdatedAt: Option[LocalDateTime]) = dto.PermissionToUpdate(
      isActive = 1.some,
      updatedBy = arg.createdBy,
      updatedAt = arg.createdAt,
      lastUpdatedAt = lastUpdatedAt)

  }

  implicit class PermissionCriteriaAdapter(val arg: PermissionCriteria) extends AnyVal {
    def asDao(isActive: Boolean = true) = dto.PermissionCriteria(
      id = arg.id.map(x ⇒ CriteriaField("id", x.underlying,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      businessId = arg.businessId.map(x ⇒ CriteriaField("buId", x.underlying,
        if (arg.partialMatchFields.contains("business_id")) MatchTypes.Partial else MatchTypes.Exact)),
      roleId = arg.roleId.map(x ⇒ CriteriaField("roleId", x.underlying,
        if (arg.partialMatchFields.contains("role_id")) MatchTypes.Partial else MatchTypes.Exact)),
      userId = arg.userId.map(x ⇒ CriteriaField("userId", x.underlying,
        if (arg.partialMatchFields.contains("user_id")) MatchTypes.Partial else MatchTypes.Exact)),
      scopeId = arg.scopeId.map(x ⇒ CriteriaField("scopeId", x.underlying,
        if (arg.partialMatchFields.contains("scope_id")) MatchTypes.Partial else MatchTypes.Exact)),
      isActive = CriteriaField("is_active", isActive.toInt).some)
  }

  implicit class PermissionUpdateDtoAdapter(val arg: PermissionToUpdate) extends AnyVal {
    def asDao(isActive: Option[Boolean] = None, maybeMissingLastUpdatedAt: Option[LocalDateTime] = None) = {
      val (buId, roleId, userId) = arg.permissionKey.fold((none[String], none[String], none[String]))(_ match {
        case BusinessUnitAndRolePermissionKey(buId, roleId) ⇒
          (buId.toString.some, roleId.toString.some, none[String])
        case UserPermissionKey(userId) ⇒
          (none[String], none[String], userId.toString.some)
      })

      dto.PermissionToUpdate(
        businessUnitId = buId,
        roleId = roleId,
        userId = userId,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt,
        lastUpdatedAt = arg.lastUpdatedAt.orElse(maybeMissingLastUpdatedAt),
        isActive = isActive.map(_.toInt))
    }
  }
}
