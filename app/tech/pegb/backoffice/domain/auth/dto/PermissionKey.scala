package tech.pegb.backoffice.domain.auth.dto

import java.util.UUID

sealed trait PermissionKey {
  def keyType: String
}

object PermissionKeys {
  final case class BusinessUnitAndRolePermissionKey(buId: UUID, roleId: UUID) extends PermissionKey {
    override val keyType = "business_unit_id and role_id permission key"
  }
  final case class UserPermissionKey(userId: UUID) extends PermissionKey {
    override val keyType = "user_id permission key"
  }
}
