package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class BusinessUnitToUpdate(
    name: Option[String],
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])

object BusinessUnitToUpdate {

  val empty = BusinessUnitToUpdate(
    name = None,
    updatedBy = "",
    updatedAt = LocalDateTime.now(),
    lastUpdatedAt = None)
}
