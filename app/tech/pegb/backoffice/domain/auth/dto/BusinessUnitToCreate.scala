package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime

case class BusinessUnitToCreate(
    name: String,
    createdBy: String,
    createdAt: LocalDateTime)

object BusinessUnitToCreate {

  val empty = BusinessUnitToCreate(
    name = "",
    createdBy = "",
    createdAt = LocalDateTime.now())
}
