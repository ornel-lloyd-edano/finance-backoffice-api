package tech.pegb.backoffice.dao.document.dto

import java.time.LocalDateTime
import java.util.UUID

case class DocumentToCreate(
    customerId: Option[UUID],
    walletApplicationId: Option[UUID],
    businessApplicationId: Option[UUID],
    documentType: String,
    documentIdentifier: Option[String],
    fileName: Option[String],
    purpose: String,
    createdBy: String,
    createdAt: LocalDateTime)

object DocumentToCreate {
  val InitialStatus = "pending"
}
