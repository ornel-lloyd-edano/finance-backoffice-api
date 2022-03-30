package tech.pegb.backoffice.mapping.dao.domain.application

import tech.pegb.backoffice.dao
import tech.pegb.backoffice.domain.application.model.{ApplicationStatus, WalletApplication}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn

import scala.util.Try

object Implicits {
  implicit class WalletApplicationAdapter(val arg: dao.application.entity.WalletApplication) extends AnyVal {
    def asDomain: Try[WalletApplication] = Try {

      WalletApplication(
        id = arg.uuid,
        customerId = arg.userUuid,
        fullName = arg.fullNameUpdated.orElse(arg.fullNameOriginal),
        nationalId = arg.personIdUpdated.orElse(arg.personIdOriginal),
        msisdn = arg.msisdn.map(Msisdn(_)),
        status = ApplicationStatus(arg.status),
        applicationStage = arg.applicationStage,
        checkedBy = arg.checkedBy,
        checkedAt = arg.checkedAt,
        rejectionReason = arg.rejectionReason,
        totalScore = arg.totalScore,

        fullNameScore = arg.fullNameScore,
        fullNameOriginal = arg.fullNameOriginal,
        fullNameUpdated = arg.fullNameUpdated,

        birthdateScore = arg.birthdateScore,
        birthdateOriginal = arg.birthdateOriginal,
        birthdateUpdated = arg.birthdateUpdated,

        birthplaceScore = arg.birthplaceScore,
        birthplaceOriginal = arg.birthplaceOriginal,
        birthplaceUpdated = arg.birthplaceUpdated,

        genderScore = arg.genderScore,
        genderOriginal = arg.genderOriginal,
        genderUpdated = arg.genderUpdated,

        nationalityScore = arg.nationalityScore,
        nationalityOriginal = arg.nationalityOriginal,
        nationalityUpdated = arg.nationalityUpdated,

        personIdScore = arg.personIdScore,
        personIdOriginal = arg.personIdOriginal,
        personIdUpdated = arg.personIdUpdated,

        documentNumberScore = arg.documentNumberScore,
        documentNumberOriginal = arg.documentNumberOriginal,
        documentNumberUpdated = arg.documentNumberUpdated,
        documentType = arg.documentType,
        documentModel = arg.documentModel,

        createdAt = arg.createdAt,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy)
    }
  }

}
