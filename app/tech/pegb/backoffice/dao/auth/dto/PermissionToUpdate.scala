package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.auth.sql.PermissionSqlDao._

case class PermissionToUpdate(
    businessUnitId: Option[String] = None,
    roleId: Option[String] = None,
    userId: Option[String] = None,
    scopeId: Option[String] = None,
    isActive: Option[Int] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)

  businessUnitId.foreach(x ⇒ append(cBuId → x))
  roleId.foreach(x ⇒ append(cRoleId → x))
  userId.foreach(x ⇒ append(cUserId → x))
  scopeId.foreach(x ⇒ append(cScopeId → x))
  isActive.foreach(x ⇒ append(cIsActive → x))
}
