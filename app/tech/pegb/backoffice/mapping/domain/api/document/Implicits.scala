package tech.pegb.backoffice.mapping.domain.api.document

import tech.pegb.backoffice.api.document.dto.DocumentToRead
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class DocumentToReadAdapter(val arg: Document) extends AnyVal {
    def asApi = DocumentToRead(
      id = arg.id,
      customerId = arg.customerId,
      applicationId = arg.applicationId,
      documentType = arg.documentType.toString.toLowerCase,
      documentIdentifier = arg.documentIdentifier,
      purpose = arg.purpose,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      createdBy = arg.createdBy,
      status = arg.status.toString.toLowerCase,
      rejectionReason = arg.rejectionReason,
      checkedAt = arg.checkedAt.map(_.toZonedDateTimeUTC),
      checkedBy = arg.checkedBy,
      uploadedAt = arg.fileUploadedAt.map(_.toZonedDateTimeUTC),
      uploadedBy = arg.fileUploadedBy,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }
}
