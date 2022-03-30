package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class CardType(
    id: Int,
    cardTypeName: String,
    description: String,
    isActive: Boolean,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

case class CardApplicationOperationType(
    id: Int,
    operationTypeName: String,
    description: String,
    isActive: Boolean,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

case class CardApplicationStatus(
    id: Int,
    applicationStatusName: String,
    description: String,
    isActive: Boolean,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

case class CardApplication(
    id: String,
    userId: String,
    operationType: String,
    cardType: String,
    nameOnCard: String,
    cardPin: String,
    deliveryAddress: String,
    status: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object CardApplication {
  implicit val f = Json.format[CardApplication]
}

case class CardApplicationGetCriteria(
    userId: Option[String] = None,
    operationType: Option[String] = None,
    cardType: Option[String] = None,
    status: Option[String] = None,
    createdDateFrom: Option[LocalDateTime] = None,
    createdDateTo: Option[LocalDateTime] = None,
    createdBy: Option[String] = None,
    updatedDateFrom: Option[LocalDateTime] = None,
    updatedDateTo: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None)
