package tech.pegb.backoffice.mapping.domain.api.application

import tech.pegb.backoffice.api.application.dto.{WalletApplicationDetail, WalletApplicationToRead}
import tech.pegb.backoffice.domain.application.model.WalletApplication
import tech.pegb.backoffice.util.Implicits._
object Implicits {

  implicit class WalletApplicationAdapter(val arg: WalletApplication) extends AnyVal {
    def asApi: WalletApplicationToRead = {
      WalletApplicationToRead(
        id = arg.id.toString,
        customerId = arg.customerId.toString,
        fullName = arg.fullName,
        personId = arg.nationalId,
        msisdn = arg.msisdn.map(_.underlying),
        status = arg.status.underlying.toLowerCase,
        appliedAt = arg.createdAt.toZonedDateTimeUTC,
        applicationStage = arg.applicationStage.toLowerCase,
        checkedAt = arg.checkedAt.map(_.toZonedDateTimeUTC),
        checkedBy = arg.checkedBy,
        reasonIfRejected = arg.rejectionReason,
        totalScore = arg.totalScore,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

  implicit class WalletApplicationDetailsAdapter(val arg: WalletApplication) extends AnyVal {
    def asDetailApi: WalletApplicationDetail = {
      WalletApplicationDetail(
        id = arg.id.toString,
        customerId = arg.customerId.toString,
        fullName = arg.fullName,
        personId = arg.nationalId,

        msisdn = arg.msisdn.map(_.underlying),
        status = arg.status.underlying.toLowerCase,
        applicationStage = arg.applicationStage.toLowerCase,
        appliedAt = arg.createdAt.toZonedDateTimeUTC,
        checkedAt = arg.checkedAt.map(_.toZonedDateTimeUTC),
        checkedBy = arg.checkedBy,
        reasonIfRejected = arg.rejectionReason,
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

        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

}
