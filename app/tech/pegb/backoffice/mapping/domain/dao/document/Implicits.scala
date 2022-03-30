package tech.pegb.backoffice.mapping.domain.dao.document

import tech.pegb.backoffice.dao.document.dto.{DocumentCriteria, DocumentToCreate, DocumentToUpdate}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.application.model.{ApplicationType, ApplicationTypes}
import tech.pegb.backoffice.domain.document.dto.{DocumentToApprove, DocumentToPersist, DocumentToReject, DocumentToUpload, DocumentCriteria ⇒ DomainDocumentCriteria, DocumentToCreate ⇒ DomainDocToCreate}
import tech.pegb.backoffice.domain.document.model.DocumentStatuses

object Implicits {

  implicit class DocumentCriteriaAdapter(val arg: DomainDocumentCriteria) extends AnyVal {
    def asDao(): DocumentCriteria = {
      val customerId = arg.customerId.map(userId ⇒
        CriteriaField("uuid", userId.underlying,
          if (arg.partialMatchFields.contains("customer_id")) MatchTypes.Partial else MatchTypes.Exact))
      val individualUserFullName = arg.customerFullName.map(fullName ⇒ {
        CriteriaField("fullname", fullName,
          if (arg.partialMatchFields.contains("customer_full_name")) MatchTypes.Partial else MatchTypes.Exact)
      })
      val msisdn = arg.customerMsisdn.map(msisdn ⇒ {
        CriteriaField("msisdn", msisdn.underlying,
          if (arg.partialMatchFields.contains("msisdn")) MatchTypes.Partial else MatchTypes.Exact)
      })

      DocumentCriteria(
        customerId = customerId,
        individualUserFullName = individualUserFullName,
        individualUserMsisdn = msisdn,
        documentType = arg.documentType.map(_.toString),
        documentIdentifier = arg.documentIdentifier,
        status = arg.status.map(_.toString),
        applicationId = arg.walletApplicationId,
        businessUserApplicationId = arg.businessApplicationId,
        filename = arg.filename.map(fn ⇒ CriteriaField("file_name", fn,
          if (arg.partialMatchFields.contains("file_name")) MatchTypes.Partial else MatchTypes.Exact)),
        checkedBy = arg.checkedBy,
        checkedAtStartingFrom = arg.checkedAtStartingFrom,
        checkedAtUpTo = arg.checkedAtUpTo,
        createdBy = arg.createdBy,
        createdAtStartingFrom = arg.createdAtStartingFrom,
        createdAtUpTo = arg.createdAtUpTo)
    }
  }

  implicit class DocumentToCreateAdapter(val arg: DomainDocToCreate) extends AnyVal {
    def asDao(applicationType: ApplicationType = ApplicationTypes.WalletApplication) = DocumentToCreate(
      customerId = arg.customerId,
      walletApplicationId = if (applicationType.isWallet) arg.applicationId else None,
      businessApplicationId = if (applicationType.isBusiness) arg.applicationId else None,
      fileName = arg.fileName,
      documentType = arg.documentType.toString.toLowerCase,
      documentIdentifier = arg.documentIdentifier,
      purpose = arg.purpose,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt)
  }

  implicit class DocumentToApproveAdapter(val arg: DocumentToApprove) extends AnyVal {
    def asDao = DocumentToUpdate(
      status = Some(DocumentStatuses.Approved.toString),
      checkedBy = Option(arg.approvedBy),
      checkedAt = Option(arg.approvedAt),
      updatedBy = Option(arg.approvedBy),
      updatedAt = Option(arg.approvedAt),
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class DocumentToRejectAdapter(val arg: DocumentToReject) extends AnyVal {
    def asDao = DocumentToUpdate(
      status = Some(DocumentStatuses.Rejected.toString),
      rejectionReason = Option(arg.reason),
      checkedBy = Option(arg.rejectedBy),
      checkedAt = Option(arg.rejectedAt),
      updatedBy = Option(arg.rejectedBy),
      updatedAt = Option(arg.rejectedAt),
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class DocumentToUploadAdapter(val arg: DocumentToUpload) extends AnyVal {
    def asDao = DocumentToUpdate(
      status = Some(DocumentStatuses.Ongoing.toString),
      fileUploadedBy = Some(arg.fileUploadedBy),
      fileUploadedAt = Some(arg.fileUploadedAt),
      updatedAt = Some(arg.fileUploadedAt),
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class DocumentToPersistAdapter(val arg: DocumentToPersist) extends AnyVal {
    def asDao = DocumentToUpdate(
      status = Some(DocumentStatuses.Approved.toString),
      updatedBy = Some(arg.persistedBy),
      filePersistedAt = Some(arg.persistedAt),
      updatedAt = Some(arg.persistedAt),
      lastUpdatedAt = arg.lastUpdatedAt)
  }
}
