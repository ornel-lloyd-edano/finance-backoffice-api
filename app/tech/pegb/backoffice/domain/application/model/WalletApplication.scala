package tech.pegb.backoffice.domain.application.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn

case class WalletApplication(
    id: UUID,
    customerId: UUID,
    fullName: Option[String],
    nationalId: Option[String],
    msisdn: Option[Msisdn],
    status: ApplicationStatus,
    applicationStage: String,
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    rejectionReason: Option[String],
    totalScore: Option[BigDecimal],

    fullNameScore: Option[BigDecimal],
    fullNameOriginal: Option[String],
    fullNameUpdated: Option[String],

    birthdateScore: Option[BigDecimal],
    birthdateOriginal: Option[String],
    birthdateUpdated: Option[String],

    birthplaceScore: Option[BigDecimal],
    birthplaceOriginal: Option[String],
    birthplaceUpdated: Option[String],

    genderScore: Option[BigDecimal],
    genderOriginal: Option[String],
    genderUpdated: Option[String],

    nationalityScore: Option[BigDecimal],
    nationalityOriginal: Option[String],
    nationalityUpdated: Option[String],

    personIdScore: Option[BigDecimal],
    personIdOriginal: Option[String],
    personIdUpdated: Option[String],

    documentNumberScore: Option[BigDecimal],
    documentNumberOriginal: Option[String],
    documentNumberUpdated: Option[String],
    documentType: Option[String],
    documentModel: Option[String],

    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object WalletApplication {

  val getEmpty =
    WalletApplication(
      id = UUID.randomUUID(),
      customerId = UUID.randomUUID(),
      fullName = None,
      nationalId = None,
      msisdn = None,
      status = ApplicationStatus("PENDING"),
      applicationStage = "stage",
      checkedBy = None,
      checkedAt = None,
      rejectionReason = None,
      totalScore = None,

      fullNameScore = None,
      fullNameOriginal = None,
      fullNameUpdated = None,

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
      documentModel = None,

      createdAt = LocalDateTime.of(2019, 2, 11, 0, 0, 0),
      createdBy = "test",
      updatedAt = None,
      updatedBy = None)
}
