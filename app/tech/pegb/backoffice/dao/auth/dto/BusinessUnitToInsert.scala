package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

case class BusinessUnitToInsert(
    name: String,
    isActive: Int,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])

object BusinessUnitToInsert {

  val empty = BusinessUnitToInsert(
    name = "",
    isActive = 1,
    updatedBy = None,
    createdBy = "",
    createdAt = LocalDateTime.now,
    updatedAt = None)
}

