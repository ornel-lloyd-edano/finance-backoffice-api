package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime
import tech.pegb.backoffice.dao.auth.sql.RoleSqlDao._

import tech.pegb.backoffice.dao.GenericUpdateSql

case class RoleToUpdate(
    updatedBy: String,
    updatedAt: LocalDateTime,
    name: Option[String] = None,
    level: Option[Int] = None,
    isActive: Option[Int] = None,
    lastUpdatedAt: Option[LocalDateTime] = None) extends GenericUpdateSql {
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)
  lastUpdatedAt.foreach(paramsBuilder += cLastUpdatedAt → _)

  name.foreach(x ⇒ append(cName → x))
  level.foreach(x ⇒ append(cLevel → x))
  isActive.foreach(x ⇒ append(cIsActive → x))
}
