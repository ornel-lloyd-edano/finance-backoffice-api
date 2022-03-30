package tech.pegb.backoffice.domain.auth.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.model.CreateUpdateValidation
import tech.pegb.backoffice.util.Implicits._

case class BusinessUnit(
    id: UUID,
    name: String,
    createdBy: String,
    updatedBy: Option[String],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime]) {

  //TODO enable validations once createdAt and createdBy are not nullable in db

  assert(BusinessUnit.isValidBusinessUnitName(this.name), "name in business unit can not be empty and cannot be longer than 32 characters")
  //assert(BusinessUnit.isValidCreatedBy(this.createdBy), "created_by in business unit can not be empty")
  //assert(BusinessUnit.isValidUpdatedAt(this.updatedAt, this.createdAt), "updated_at cannot be before created_at")
  assert(BusinessUnit.isValidUpdate(this.updatedBy, this.updatedAt), "update_at or updated_by is missing")
}

object BusinessUnit extends CreateUpdateValidation {

  val empty = BusinessUnit(
    id = UUID.randomUUID(),
    name = "department_name",
    createdBy = "pegbuser",
    createdAt = LocalDateTime.now(),
    updatedBy = Some("pegbuser"),
    updatedAt = Some(LocalDateTime.now()))

  def isValidBusinessUnitName(name: String): Boolean = {
    name.trim.hasSomething && name.trim.length <= 32
  }
}
