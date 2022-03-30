package tech.pegb.backoffice.domain.document.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.BaseService.{BatchValidatedServiceResponse, ServiceResponse}
import tech.pegb.backoffice.domain.{BatchValidatable, ServiceError}
import tech.pegb.backoffice.util.Implicits._
import Document._

final case class Document(
    id: UUID,
    customerId: Option[UUID],
    applicationId: Option[UUID],
    documentName: Option[String],
    documentType: DocumentType,
    documentIdentifier: Option[String],
    purpose: String,
    status: DocumentStatus,
    rejectionReason: Option[String],
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    fileUploadedAt: Option[LocalDateTime],
    fileUploadedBy: Option[String],
    updatedAt: Option[LocalDateTime]) extends BatchValidatable[Document] {

  def validate: BatchValidatedServiceResponse[Document] = {
    val errors = Seq(
      if (applicationId.isEmpty)
        Left(ServiceError.validationError(s"Document with id [$id] is missing the application id"))
      else Right(()),
      DocumentTypes.validate(documentType.toString),
      checkPurpose(purpose),
      checkStatus(status, rejectionReason, checkedBy, checkedAt),
      checkUpload(fileUploadedBy, fileUploadedAt, status))
      .collect { case Left(err) ⇒ err }

    if (errors.nonEmpty) Left(errors) else Right(this)
  }

  def getFileType: Option[String] = {
    documentName.map(_.split(s"\\$fileExtensionSeparator").last)
  }

}

object Document {
  final val Pending = "pending"
  final val Approved = "approved"
  final val Rejected = "rejected"
  private val fileExtensionSeparator = "."
  val validFileTypes = Seq("jpeg", "jpg", "png", "pdf", "rar", "zip")

  def checkFilename(filename: String): ServiceResponse[Unit] = {
    val fileType = filename.split(s"\\$fileExtensionSeparator").last
    val fileNameOnly = filename.split(s"\\$fileExtensionSeparator").head

    (filename.trim.nonEmpty, filename.contains(fileExtensionSeparator), fileNameOnly.nonEmpty, validFileTypes.contains(fileType.toLowerCase)) match {
      case (false, _, _, _) ⇒
        Left(ServiceError.validationError("empty filename"))
      case (_, false, _, _) ⇒
        Left(ServiceError.validationError(s"Filename [$filename] does not have file type extension"))
      case (_, _, false, _) ⇒
        Left(ServiceError.validationError(s"Malformed filename [$filename] is only a file type extension"))
      case (_, _, _, false) ⇒
        Left(ServiceError.validationError(s"Filename extension [$fileType] is not valid. Valid file types are: ${validFileTypes.defaultMkString}"))
      case _ ⇒ Right(())
    }
  }

  def checkPurpose(purpose: String): ServiceResponse[Unit] = {
    if (purpose.hasSomething) Right(()) else Left(ServiceError.validationError("empty purpose"))
  }

  def checkStatus(
    status: DocumentStatus,
    rejectionReason: Option[String],
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime]): ServiceResponse[Unit] = {
    val underlyingStatus = status.toString
    (
      underlyingStatus === Pending && (checkedBy.nonEmpty || checkedAt.nonEmpty),
      underlyingStatus === Rejected && rejectionReason.isEmpty,
      rejectionReason.nonEmpty && underlyingStatus.toLowerCase != Rejected,
      (underlyingStatus === Rejected || underlyingStatus === Approved) && (checkedBy.isEmpty || checkedAt.isEmpty)) match {
        case (true, _, _, _) ⇒
          Left(ServiceError.validationError("checkedBy and/or checkedAt cannot have value if status is pending"))
        case (_, true, _, _) ⇒
          Left(ServiceError.validationError("Status is rejected but rejection reason is empty"))
        case (_, _, true, _) ⇒
          Left(ServiceError.validationError("Status is not rejected but rejection reason has value"))
        case (_, _, _, true) ⇒
          Left(ServiceError.validationError(s"Status is [$underlyingStatus] but checkedAt and/or checkedBy is empty"))
        case _ ⇒ Right(())
      }

  }

  def checkUpload(
    uploadedBy: Option[String],
    uploadedAt: Option[LocalDateTime],
    status: DocumentStatus): ServiceResponse[Unit] = {
    (
      uploadedBy.isEmpty && uploadedAt.isDefined,
      uploadedBy.isDefined && uploadedAt.isEmpty,
      status.toString === Approved && (uploadedBy.isEmpty || uploadedAt.isEmpty)) match {
        case (true, _, _) ⇒
          Left(ServiceError.validationError("Empty uploadedBy but uploadedAt is not"))
        case (_, true, _) ⇒
          Left(ServiceError.validationError("Empty uploadedAt but uploadedBy is not"))
        case (_, _, true) ⇒
          Left(ServiceError.validationError(s"Status cannot be approved if uploadedBy and/or uploadedAt is empty"))
        case _ ⇒
          Right(())
      }
  }

  val empty = new Document(id = UUID.randomUUID(), customerId = Some(UUID.randomUUID()), applicationId = Some(UUID.randomUUID()),
    documentName = None, documentType = DocumentTypes.fromString("some document type"),
    documentIdentifier = None, purpose = "some purpose", status = DocumentStatuses.fromString("some document status"), rejectionReason = None,
    checkedBy = None, checkedAt = None, createdBy = "some user", createdAt = LocalDateTime.now, fileUploadedAt = None, fileUploadedBy = None, updatedAt = None)
}
