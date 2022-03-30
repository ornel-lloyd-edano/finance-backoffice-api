package tech.pegb.backoffice.dao.application.dto

import java.time.LocalDateTime
import java.util.UUID

case class WalletApplicationToCreate(
    id: Option[Int],
    uuid: UUID,
    userId: Int,
    status: String,
    stage: String,
    rejectedReason: Option[String],
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    totalScore: Option[Float],

    fullnameScore: Option[Float],
    fullnameOriginal: Option[String],
    fullnameUpdated: Option[String],

    birthdateScore: Option[Float],
    birthdateOriginal: Option[LocalDateTime],
    birthdateUpdated: Option[LocalDateTime],

    birthplaceScore: Option[Float],
    birthplaceOriginal: Option[LocalDateTime],
    birthplaceUpdated: Option[LocalDateTime],

    genderScore: Option[Float],
    genderOriginal: Option[String],
    genderUpdated: Option[String],

    nationalityScore: Option[Float],
    nationalityOriginal: Option[String],
    nationalityUpdated: Option[String],

    personIdScore: Option[Float],
    personIdOriginal: Option[String],
    personIdUpdated: Option[String],

    documentNumberScore: Option[Float],
    documentNumberOriginal: Option[String],
    documentNumberUpdated: Option[String],
    documentType: Option[String],

    createdBy: String,
    createdAt: LocalDateTime,

    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])

object WalletApplicationToCreate {
  def createEmpty(
    uuid: UUID,
    createdBy: String,
    createdAt: LocalDateTime) = WalletApplicationToCreate(
    id = None,
    uuid = uuid,
    userId = -1,
    status = "pending",
    stage = "new",
    rejectedReason = None,
    checkedBy = None,
    checkedAt = None,
    totalScore = None,

    fullnameScore = None,
    fullnameOriginal = None,
    fullnameUpdated = None,

    birthdateScore = None,
    birthdateOriginal = None,
    birthdateUpdated = None,

    birthplaceScore = None,
    birthplaceOriginal = None,
    birthplaceUpdated = None,

    genderScore = None,
    genderOriginal = None,
    genderUpdated = None,

    nationalityScore = None,
    nationalityOriginal = None,
    nationalityUpdated = None,

    personIdScore = None,
    personIdOriginal = None,
    personIdUpdated = None,

    documentNumberScore = None,
    documentNumberOriginal = None,
    documentNumberUpdated = None,
    documentType = None,

    createdBy = createdBy,
    createdAt = createdAt,

    updatedBy = None,
    updatedAt = None)
}
