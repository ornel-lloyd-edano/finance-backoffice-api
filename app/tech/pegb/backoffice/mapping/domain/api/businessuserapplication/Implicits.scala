package tech.pegb.backoffice.mapping.domain.api.businessuserapplication

import tech.pegb.backoffice.api.businessuserapplication.dto._
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessUserApplication, BusinessUserApplicationAttributes}
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.mapping.domain.api.AsApiAdapter
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit object BusinessUserApplicationToReadAdapter extends AsApiAdapter[BusinessUserApplication, BusinessUserApplicationToRead] {
    def asApi(arg: BusinessUserApplication) = {
      BusinessUserApplicationToRead(
        id = arg.uuid,
        businessName = arg.businessName.underlying,
        brandName = arg.brandName.underlying,
        businessCategory = arg.businessCategory.underlying,
        stage = arg.stage.underlying,
        status = arg.status.underlying,
        userTier = arg.userTier.toString,
        businessType = arg.businessType.toString,
        registrationNumber = arg.registrationNumber.underlying,
        taxNumber = arg.taxNumber.map(_.underlying),
        registrationDate = arg.registrationDate,
        explanation = arg.explanation,
        submittedBy = arg.submittedBy,
        submittedAt = arg.submittedAt.map(_.toZonedDateTimeUTC),
        checkedBy = arg.checkedBy,
        checkedAt = arg.checkedAt.map(_.toZonedDateTimeUTC),
        validTransactionConfig = arg.getValidTransactionConfig.map(_.asApi),
        createdBy = arg.createdBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

  implicit object BusinessUserApplicationConfigToReadAdapter extends AsApiAdapter[BusinessUserApplication, BusinessUserApplicationConfigToRead] {
    def asApi(arg: BusinessUserApplication) = {
      BusinessUserApplicationConfigToRead(
        id = arg.uuid,
        status = arg.status.underlying,
        transactionConfig = arg.transactionConfig.map(_.asApi),
        accountConfig = arg.accountConfig.map(_.asApi),
        externalAccounts = arg.externalAccounts.map(_.asApi),
        createdBy = arg.createdBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        submittedBy = arg.submittedBy)
    }
  }

  implicit object BusinessUserApplicationDocToReadAdapter extends AsApiAdapter[BusinessUserApplication, BusinessUserApplicationDocumentToRead] {
    def asApi(arg: BusinessUserApplication) = {
      BusinessUserApplicationDocumentToRead(
        id = arg.uuid,
        status = arg.status.underlying,
        documents = arg.documents.map(_.asApi),
        createdBy = arg.createdBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        submittedBy = arg.submittedBy)
    }
  }

  implicit object BusinessUserApplicationContactInfoAdapter extends AsApiAdapter[BusinessUserApplication, BusinessUserApplicationContactInfoToRead] {
    def asApi(arg: BusinessUserApplication): BusinessUserApplicationContactInfoToRead = {
      BusinessUserApplicationContactInfoToRead(
        id = arg.uuid.toString,
        status = arg.status.underlying,
        contacts = arg.contactPersons.map(e ⇒
          BusinessUserApplicationContact(
            contactType = e.contactType,
            name = e.name,
            middleName = e.middleName,
            surname = e.surname,
            phoneNumber = e.phoneNumber.map(_.underlying),
            email = e.email.map(_.value),
            idType = Some(e.idType),
            isVelocityUser = e.velocityUser.isDefined,
            velocityLevel = e.velocityUser,
            isDefaultContact = e.isDefault)),
        addresses = arg.contactAddress.map(e ⇒
          BusinessUserApplicationAddress(
            addressType = e.addressType,
            country = e.country,
            city = e.city,
            postalCode = e.postalCode,
            address = e.address,
            coordinateX = e.coordinates.map(_.x),
            coordinateY = e.coordinates.map(_.y))),
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        updatedBy = arg.updatedBy,
        submittedBy = arg.submittedBy)
    }
  }

  implicit class BusinessUserApplicationToApiAdapter(val arg: BusinessUserApplication) extends AnyVal {
    def asApi[T](implicit asApiAdapter: AsApiAdapter[BusinessUserApplication, T]): T = {
      asApiAdapter.asApi(arg)
    }
  }

  implicit class TransactionConfigToReadAdapter(val arg: BusinessUserApplicationAttributes.TransactionConfig) extends AnyVal {
    def asApi: TransactionConfigToRead = TransactionConfigToRead(
      transactionType = arg.transactionType.underlying,
      currencyCode = arg.currency.getCurrencyCode)
  }

  implicit class AccountConfigToReadAdapter(val arg: BusinessUserApplicationAttributes.AccountConfig) extends AnyVal {
    def asApi: AccountConfigToRead = AccountConfigToRead(
      accountType = arg.accountType.underlying,
      accountName = arg.accountName.underlying,
      currencyCode = arg.currency.getCurrencyCode,
      isDefault = arg.isDefault)
  }

  implicit class ExternalAccountToReadAdapter(val arg: BusinessUserApplicationAttributes.ExternalAccount) extends AnyVal {
    def asApi: ExternalAccountToRead = ExternalAccountToRead(
      provider = arg.provider.underlying,
      accountNumber = arg.accountNumber.underlying,
      accountHolder = arg.accountHolder.underlying,
      currencyCode = arg.currency.getCurrencyCode)
  }

  implicit class DocumentToReadAdapter(val arg: Document) extends AnyVal {
    def asApi = {
      SimpleDocumentToRead(
        id = arg.id,
        applicationId = arg.applicationId,
        filename = arg.documentName.getOrElse("Not Available"),
        documentType = arg.documentType.toString)
    }
  }
}
