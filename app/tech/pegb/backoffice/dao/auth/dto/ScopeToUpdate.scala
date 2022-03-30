package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.auth.sql.ScopeSqlDao._

case class ScopeToUpdate(
    description: Option[String] = None,
    isActive: Option[Int] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)

  description.foreach(x ⇒ append(cDescription → x))
  isActive.foreach(x ⇒ append(cIsActive → x))

}
