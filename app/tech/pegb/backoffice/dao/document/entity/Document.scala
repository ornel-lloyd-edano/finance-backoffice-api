package tech.pegb.backoffice.dao.document.entity

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.Json

case class Document(
    id: Int,
    uuid: UUID,
    customerId: Option[UUID] = None,
    walletApplicationId: Option[UUID] = None,
    businessUserApplicationId: Option[UUID] = None,
    documentType: String,
    documentIdentifier: Option[String] = None,
    purpose: String,
    status: String,
    rejectionReason: Option[String] = None,
    checkedBy: Option[String] = None,
    checkedAt: Option[LocalDateTime] = None,
    fileName: Option[String] = None,
    fileUploadedBy: Option[String] = None,
    fileUploadedAt: Option[LocalDateTime] = None,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None)

object Document {
  implicit val f = Json.format[Document]

  def apply(id: Int, uuid: UUID, docType: String, purpose: String, status: String, createdBy: String, createdAt: LocalDateTime) = new Document(id = id, uuid = uuid, documentType = docType, purpose = purpose, status = status, createdBy = createdBy, createdAt = createdAt)
}
