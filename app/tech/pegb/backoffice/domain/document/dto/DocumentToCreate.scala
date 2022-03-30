package tech.pegb.backoffice.domain.document.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.domain.document.model.{Document, DocumentType, DocumentTypes}

case class DocumentToCreate(
    customerId: Option[UUID],
    applicationId: Option[UUID],
    fileName: Option[String],
    documentType: DocumentType,
    documentIdentifier: Option[String],
    purpose: String,
    createdBy: String,
    createdAt: LocalDateTime) extends Validatable[Unit] {

  override def validate: ServiceResponse[Unit] = {
    for {
      _ ← Document.checkPurpose(purpose)
      _ ← fileName.map(Document.checkFilename(_)).getOrElse(Right(()))
      _ ← DocumentTypes.validate(documentType.toString)
      _ ← if (createdBy.trim.isEmpty) Left(ServiceError.validationError("created_by cannot be empty")) else Right(())
    } yield {
      ()
    }
  }

}
