package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

object CustomerAttributes {

  case class CustomerSubscription(
      subscriptionName: String,
      description: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class CustomerTier(
      tierName: String,
      description: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class CustomerSegment(
      segmentName: String,
      description: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class CustomerType(
      customerTypeName: String,
      description: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class BusinessUserType(
      businessUserTypeName: String,
      description: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class CustomerStatus(
      statusName: String,
      description: Option[String],
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  object CustomerStatus {
    def getEmpty = CustomerStatus(statusName = "", description = None, isActive = true,
      createdAt = LocalDateTime.now, createdBy = "", updatedAt = None, updatedBy = None)
  }

}

