package tech.pegb.backoffice.mapping.dao.domain.document

import tech.pegb.backoffice.dao.document.entity.{Document ⇒ DaoDocument}
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}

object Implicits {

  implicit class DocumentAdapter(val arg: DaoDocument) extends AnyVal {
    def asDomain: Document = Document(
      id = arg.uuid,
      customerId = arg.customerId,
      applicationId = (arg.walletApplicationId, arg.businessUserApplicationId) match {
        case (Some(walletApplicationId), _) ⇒ Some(walletApplicationId)
        case (_, Some(businessUserApplicationId)) ⇒ Some(businessUserApplicationId)
        case _ ⇒ None
      },
      documentName = arg.fileName,
      documentType = DocumentTypes.fromString(arg.documentType),
      documentIdentifier = arg.documentIdentifier,
      purpose = arg.purpose,
      status = DocumentStatuses.fromString(arg.status),
      rejectionReason = arg.rejectionReason,
      checkedBy = arg.checkedBy,
      checkedAt = arg.checkedAt,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      fileUploadedAt = arg.fileUploadedAt,
      fileUploadedBy = arg.fileUploadedBy,
      updatedAt = arg.updatedAt)
  }

}
