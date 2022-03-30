package tech.pegb.backoffice.mapping.dao.domain.businessuserapplication

import java.util.{Currency, UUID}

import tech.pegb.backoffice.dao.businessuserapplication.entity
import tech.pegb.backoffice.dao.businessuserapplication.entity.{BUApplicPrimaryAddress, BUApplicPrimaryContact, BusinessUserApplication}
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountType}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.model
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.businessuserapplication.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, NameAttribute}
import tech.pegb.backoffice.domain.transaction.model.TransactionType

import scala.util.Try

object Implicits {

  implicit class BusinessUserApplicPrimaryContactAdapter(val arg: BUApplicPrimaryContact) extends AnyVal {
    def asDomain =
      ContactPerson(
        contactType = arg.contactType,
        name = arg.name,
        middleName = arg.middleName,
        surname = arg.surname,
        phoneNumber = Try(Msisdn(arg.phoneNumber)).toOption,
        email = Try(Email(arg.email)).toOption,
        idType = arg.idType, //TODO objectify
        velocityUser = if (arg.isVelocityUser) arg.velocityLevel else None,
        isDefault = arg.isDefaultContact)
  }

  implicit class BusinessUserApplicAddressAdapter(val arg: BUApplicPrimaryAddress) extends AnyVal {
    def asDomain(country: String) = ContactAddress(
      addressType = arg.addressType,
      country = country,
      city = arg.city,
      postalCode = arg.postalCode,
      address = arg.address.getOrElse("Not Available"),
      coordinates = if (arg.coordinateX.isEmpty || arg.coordinateY.isEmpty) None
      else Some(AddressCoordinates(arg.coordinateX.get, arg.coordinateY.get)))
  }

  implicit class BusinessUserApplicationAdapter(val arg: BusinessUserApplication) extends AnyVal {
    def asDomain(defaultCurrency: Currency): Try[model.BusinessUserApplication] = Try {
      model.BusinessUserApplication(
        id = arg.id,
        uuid = UUID.fromString(arg.uuid),
        businessName = NameAttribute(arg.businessName),
        brandName = NameAttribute(arg.brandName),
        businessCategory = BusinessCategory(arg.businessCategory),
        stage = ApplicationStage(arg.stage),
        status = ApplicationStatus(arg.status),
        userTier = BusinessUserTiers.fromString(arg.userTier),
        businessType = BusinessTypes.fromString(arg.businessType),
        registrationNumber = RegistrationNumber(arg.registrationNumber),
        taxNumber = arg.taxNumber.map(TaxNumber(_)),
        registrationDate = arg.registrationDate,
        explanation = arg.explanation,
        transactionConfig = Nil,
        accountConfig = Nil,
        externalAccounts = Nil,
        contactPersons = Nil,
        contactAddress = Nil,
        defaultCurrency = defaultCurrency,
        submittedBy = arg.submittedBy,
        submittedAt = arg.submittedAt,
        checkedBy = arg.checkedBy,
        checkedAt = arg.checkedAt,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt)
    }
  }

  implicit class TransactionConfigAdapter(val arg: entity.TransactionConfig) extends AnyVal {
    def asDomain: Try[TransactionConfig] = Try {
      TransactionConfig(
        transactionType = TransactionType(arg.transactionType),
        currency = Currency.getInstance(arg.currencyCode))
    }
  }

  implicit class AccountConfigAdapter(val arg: entity.AccountConfig) extends AnyVal {
    def asDomain: Try[AccountConfig] = Try {
      AccountConfig(
        accountType = AccountType(arg.accountType),
        accountName = NameAttribute(arg.accountName),
        currency = Currency.getInstance(arg.currencyCode),
        isDefault = arg.isDefault)
    }
  }

  implicit class ExternalAccountAdapter(val arg: entity.ExternalAccount) extends AnyVal {
    def asDomain: Try[ExternalAccount] = Try {
      ExternalAccount(
        provider = NameAttribute(arg.provider),
        accountNumber = AccountNumber(arg.accountNumber),
        accountHolder = NameAttribute(arg.accountHolder),
        currency = Currency.getInstance(arg.currencyCode))
    }
  }
}
