package tech.pegb.backoffice.dao.customer.entity

import java.time.LocalDateTime

object ExtraAttributeRequirements {

  case class ExtraAttributeType(
      attributeName: String,
      description: Option[String],
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case object ExtraAttributeType {
    def getEmpty = ExtraAttributeType(attributeName = "", description = None, isActive = true, createdAt = LocalDateTime.now, createdBy = "", updatedAt = None, updatedBy = None)
  }

  case class ExtraAttributeTypeToCreate(
      attributeName: String,
      description: String,
      createdAt: LocalDateTime,
      createdBy: String)

  case class UserExtraAttribute(
      userId: Int,
      userUuid: String,
      extraAttributeName: String,
      attributeValue: String,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  object UserExtraAttribute {
    def getEmpty = UserExtraAttribute(userId = 0, userUuid = "", extraAttributeName = "", attributeValue = "",
      createdAt = LocalDateTime.now, createdBy = "",
      updatedAt = None, updatedBy = None)
  }

  case class UserExtraAttributeToAdd(
      userId: String,
      attributeName: String,
      attributeValue: String,
      createdAt: LocalDateTime,
      createdBy: String)

  case class CardApplicationRequirement(
      cardApplicationId: String,
      extraAttributeId: String,
      attributeValue: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])
}
