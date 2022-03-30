package tech.pegb.backoffice.domain.document.dto

import java.time.LocalDate
import java.util.UUID

import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.domain.document.model.{DocumentStatus, DocumentStatuses, DocumentType, DocumentTypes}
import tech.pegb.backoffice.util.UUIDLike

case class DocumentCriteria(
    customerId: Option[UUIDLike] = None,
    customerFullName: Option[String] = None,
    customerMsisdn: Option[Msisdn] = None,
    documentType: Option[DocumentType] = None,
    documentIdentifier: Option[String] = None,
    status: Option[DocumentStatus] = None,
    walletApplicationId: Option[UUID] = None,
    businessApplicationId: Option[UUID] = None,
    filename: Option[String] = None,
    checkedBy: Option[String] = None,
    checkedAtStartingFrom: Option[LocalDate] = None,
    checkedAtUpTo: Option[LocalDate] = None,
    createdBy: Option[String] = None,
    createdAtStartingFrom: Option[LocalDate] = None,
    createdAtUpTo: Option[LocalDate] = None,
    partialMatchFields: Set[String] = Set.empty) extends Validatable[Unit] {

  def validate = {
    for {
      _ ← documentType.map(d ⇒ DocumentTypes.validate(d.toString)).getOrElse(Right(()))
      _ ← status.map(s ⇒ DocumentStatuses.validate(s.toString)).getOrElse(Right(()))
    } yield {
      Right(())
    }
  }
}

object DocumentCriteria {
  val Empty = DocumentCriteria(
    customerId = None,
    customerFullName = None,
    customerMsisdn = None,
    documentType = None,
    documentIdentifier = None,
    status = None,
    walletApplicationId = None,
    checkedBy = None,
    checkedAtStartingFrom = None,
    checkedAtUpTo = None,
    createdBy = None,
    createdAtStartingFrom = None,
    createdAtUpTo = None)
}
