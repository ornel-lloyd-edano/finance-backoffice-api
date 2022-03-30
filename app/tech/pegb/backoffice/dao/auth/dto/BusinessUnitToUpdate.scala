package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.auth.sql.BusinessUnitSqlDao._

case class BusinessUnitToUpdate(
    name: Option[String],
    isActive: Option[Int],
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)
  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  name.foreach(name ⇒ append(cName → name))
  isActive.foreach(isActive ⇒ append(cIsActive → isActive))

}

object BusinessUnitToUpdate {

  val empty = BusinessUnitToUpdate(
    name = None,
    isActive = None,
    updatedBy = "",
    updatedAt = LocalDateTime.now(),
    lastUpdatedAt = None)
}
