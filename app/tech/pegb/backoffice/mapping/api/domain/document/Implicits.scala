package tech.pegb.backoffice.mapping.api.domain.document

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import tech.pegb.backoffice.api.businessuserapplication.dto.DocumentMetadataToCreate
import tech.pegb.backoffice.api.document.dto.{DocumentToCreate ⇒ ApiDocumentToCreate}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.domain.document.dto.{DocumentCriteria, DocumentToApprove, DocumentToCreate, DocumentToReject}
import tech.pegb.backoffice.domain.document.model.{DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object Implicits {

  implicit class ApplicationIdToDocumentCriteriaAdapter(val arg: UUID) extends AnyVal {
    def asDomain = DocumentCriteria(walletApplicationId = Option(arg))
  }

  implicit class DocumentToCreateAdapter(val arg: ApiDocumentToCreate) extends AnyVal {
    def asDomain(createdBy: String, createdAt: ZonedDateTime): DocumentToCreate =
      DocumentToCreate(
        customerId = Some(arg.customerId),
        fileName = arg.fileName,
        documentType = DocumentTypes.fromString(arg.documentType),
        documentIdentifier = arg.documentIdentifier,
        applicationId = Some(arg.applicationId),
        purpose = arg.purpose.sanitize,
        createdBy = createdBy,
        createdAt = createdAt.toLocalDateTimeUTC)
  }

  implicit class BuDocumentToCreateAdapter(val arg: DocumentMetadataToCreate) extends AnyVal {
    def asDomain(createdBy: String, createdAt: ZonedDateTime): DocumentToCreate =
      DocumentToCreate(
        customerId = None,
        fileName = Some(arg.filename),
        documentType = DocumentTypes.fromString(arg.documentType),
        documentIdentifier = None,
        applicationId = arg.applicationId,
        purpose = "business user application requirement",
        createdBy = createdBy,
        createdAt = createdAt.toLocalDateTimeUTC)
  }

  implicit class DocumentToApproveAdapter(val arg: (UUID, String, ZonedDateTime, Option[ZonedDateTime])) extends AnyVal {
    def asDomain = DocumentToApprove(
      id = arg._1,
      approvedBy = arg._2,
      approvedAt = arg._3.toLocalDateTimeUTC,
      lastUpdatedAt = arg._4.map(_.toLocalDateTimeUTC))
  }

  implicit class DocumentToRejectAdapter(val arg: (UUID, String, ZonedDateTime, String, Option[ZonedDateTime])) extends AnyVal {
    def asDomain = DocumentToReject(
      id = arg._1,
      rejectedBy = arg._2,
      rejectedAt = arg._3.toLocalDateTimeUTC,
      reason = arg._4.sanitize,
      lastUpdatedAt = arg._5.map(_.toLocalDateTimeUTC))
  }

  private type Status = Option[String]
  private type DocType = Option[String]
  private type CustomerId = Option[UUIDLike]
  private type CustomerFullname = Option[String]
  private type CustomerMsisdn = Option[String]
  private type ApplicationId = Option[UUID]
  private type StartDate = Option[LocalDate]
  private type EndDate = Option[LocalDate]
  private type IsCheckedAt = Option[Boolean]
  private type PartialMatch = Set[String]

  implicit class DocumentCriteriaAdapter(
      val arg: (Status, DocType, CustomerId, CustomerFullname, CustomerMsisdn, ApplicationId, StartDate, EndDate, IsCheckedAt, PartialMatch)) extends AnyVal {
    def asDomain(isForWalletApplication: Option[Boolean] = None, isForBusinessApplication: Option[Boolean] = None) =
      Try(arg match {
        case (status, docType, customerId, customerFullName, customerMsisdn, applicationId, startDate, endDate, isCheckedAt, partialMatchFields) ⇒
          val dto = DocumentCriteria(
            customerId = customerId,
            customerFullName = customerFullName,
            customerMsisdn = customerMsisdn.map(Msisdn(_)),
            walletApplicationId = if (isForWalletApplication.contains(true)) applicationId else None,
            businessApplicationId = if (isForBusinessApplication.contains(true)) applicationId else None,
            status = status.map(DocumentStatuses.fromString(_)),
            documentType = docType.map(DocumentTypes.fromString(_)),
            partialMatchFields = partialMatchFields)

          if (isCheckedAt.contains(true)) {
            dto.copy(checkedAtStartingFrom = startDate, checkedAtUpTo = endDate)
          } else {
            dto.copy(createdAtStartingFrom = startDate, createdAtUpTo = endDate)
          }
      })
  }

  implicit class CustomerIdDocumentCriteriaAdapter(val arg: UUID) extends AnyVal {
    def asDocumentCriteria = DocumentCriteria(customerId = Some(UUIDLike(arg.toString)))
  }

}
