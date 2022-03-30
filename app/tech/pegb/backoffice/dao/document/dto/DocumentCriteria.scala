package tech.pegb.backoffice.dao.document.dto

import java.time.LocalDate
import java.util.UUID

import tech.pegb.backoffice.dao.model.CriteriaField

case class DocumentCriteria(
    customerId: Option[CriteriaField[String]] = None,
    individualUserFullName: Option[CriteriaField[String]] = None,
    individualUserMsisdn: Option[CriteriaField[String]] = None,
    documentType: Option[String] = None,
    documentIdentifier: Option[String] = None,
    status: Option[String] = None,
    applicationId: Option[UUID] = None,
    businessUserApplicationId: Option[UUID] = None,
    filename: Option[CriteriaField[String]] = None,
    checkedBy: Option[String] = None,
    checkedAtStartingFrom: Option[LocalDate] = None,
    checkedAtUpTo: Option[LocalDate] = None,
    createdBy: Option[String] = None,
    createdAtStartingFrom: Option[LocalDate] = None,
    createdAtUpTo: Option[LocalDate] = None)

object DocumentCriteria {
  val Empty = DocumentCriteria(
    customerId = None,
    individualUserFullName = None,
    individualUserMsisdn = None,
    documentType = None,
    documentIdentifier = None,
    status = None,
    applicationId = None,
    businessUserApplicationId = None,
    checkedBy = None,
    checkedAtStartingFrom = None,
    checkedAtUpTo = None,
    createdBy = None,
    createdAtStartingFrom = None,
    createdAtUpTo = None)
}
