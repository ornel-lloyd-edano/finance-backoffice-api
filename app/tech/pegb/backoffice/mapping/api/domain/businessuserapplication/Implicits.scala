package tech.pegb.backoffice.mapping.api.domain.businessuserapplication

import java.time.ZonedDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.api.businessuserapplication
import tech.pegb.backoffice.api.businessuserapplication.dto.{BusinessUserApplicationAddress, BusinessUserApplicationConfigToCreate, BusinessUserApplicationContact, BusinessUserApplicationToCreate}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountType}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.dto
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.businessuserapplication.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, NameAttribute}
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  implicit class BusinessUserApplicationAdapter(val arg: BusinessUserApplicationToCreate) extends AnyVal {
    def asDomain(applicationId: UUID, createdAt: ZonedDateTime, createdBy: String): Try[dto.BusinessUserApplicationToCreate] = {
      Try {
        dto.BusinessUserApplicationToCreate(
          uuid = applicationId,
          businessName = NameAttribute(arg.businessName.sanitize),
          brandName = NameAttribute(arg.brandName.sanitize),
          businessCategory = BusinessCategory(arg.businessCategory.sanitize),
          userTier = BusinessUserTiers.fromString(arg.userTier.sanitize),
          businessType = BusinessTypes.fromString(arg.businessType.sanitize),
          registrationNumber = RegistrationNumber(arg.registrationNumber.sanitize),
          taxNumber = arg.taxNumber.map(x ⇒ TaxNumber(x.sanitize)),
          registrationDate = arg.registrationDate,
          createdBy = createdBy,
          createdAt = createdAt.toLocalDateTimeUTC)
      }
    }
  }

  implicit class BusinessUserApplicationConfigToCreateAdapter(val arg: BusinessUserApplicationConfigToCreate) extends AnyVal {
    def asDomain(uuid: UUID, createdAt: ZonedDateTime, createdBy: String): Try[dto.BusinessUserApplicationConfigToCreate] = Try {
      dto.BusinessUserApplicationConfigToCreate(
        applicationUUID = uuid,
        transactionConfig = arg.transactionConfig.map(_.asDomain().get),
        accountConfig = arg.accountConfig.map(_.asDomain().get),
        externalAccounts = arg.externalAccounts.map(_.asDomain().get),
        createdBy = createdBy,
        createdAt = createdAt.toLocalDateTimeUTC)
    }
  }

  implicit class TransactionConfigAdapter(val arg: businessuserapplication.dto.TransactionConfig) extends AnyVal {
    def asDomain(): Try[TransactionConfig] = Try {
      TransactionConfig(
        transactionType = TransactionType(arg.transactionType),
        currency = Currency.getInstance(arg.currencyCode))
    }
  }

  implicit class AccountConfigAdapter(val arg: businessuserapplication.dto.AccountConfig) extends AnyVal {
    def asDomain(): Try[AccountConfig] = Try {
      AccountConfig(
        accountType = AccountType(arg.accountType),
        accountName = NameAttribute(arg.accountName),
        currency = Currency.getInstance(arg.currencyCode),
        isDefault = arg.isDefault)
    }
  }

  implicit class ExternalAccountsAdapter(val arg: businessuserapplication.dto.ExternalAccount) extends AnyVal {
    def asDomain(): Try[ExternalAccount] = Try {
      ExternalAccount(
        provider = NameAttribute(arg.provider),
        accountNumber = AccountNumber(arg.accountNumber),
        accountHolder = NameAttribute(arg.accountHolder),
        currency = Currency.getInstance(arg.currencyCode))
    }
  }

  private type BusinessName = Option[String]
  private type BrandName = Option[String]
  private type BizCategory = Option[String]
  private type Stage = Option[String]
  private type Status = Option[String]
  private type CreatedAtFrom = Option[LocalDateTimeFrom]
  private type CreatedAtTo = Option[LocalDateTimeTo]
  private type PhoneNum = Option[String]
  private type Email = Option[String]
  private type PartialMatch = Set[String]
  implicit class BusinessUserApplicationCriteriaAdapter(val arg: (BusinessName, BrandName, BizCategory, Stage, Status, CreatedAtFrom, CreatedAtTo, PhoneNum, Email, PartialMatch)) extends AnyVal {
    def asDomain(): Try[dto.BusinessUserApplicationCriteria] = Try {
      dto.BusinessUserApplicationCriteria(
        businessName = arg._1.map(x ⇒ NameAttribute(x.sanitize)),
        brandName = arg._2.map(x ⇒ NameAttribute(x.sanitize)),
        businessCategory = arg._3.map(x ⇒ BusinessCategory(x.sanitize)),
        stage = arg._4.map(x ⇒ ApplicationStage(x.sanitize)),
        status = arg._5.map(x ⇒ ApplicationStatus(x.sanitize)),
        createdAtFrom = arg._6.map(_.localDateTime),
        createdAtTo = arg._7.map(_.localDateTime),
        contactPersonsPhoneNumber = arg._8.map(_.sanitize),
        contactPersonsEmail = arg._9.map(_.sanitize),
        partialMatchFields = arg._10)
    }
  }

  implicit class ContactPersonAdapter(val arg: BusinessUserApplicationContact) extends AnyVal {
    def asDomain = Try(ContactPerson(
      contactType = arg.contactType,
      name = arg.name.sanitize,
      middleName = arg.middleName.map(_.sanitize),
      surname = arg.surname.sanitize,
      phoneNumber = arg.phoneNumber.map(Msisdn),
      email = arg.email.map(Email(_)),
      idType = arg.idType.getOrElse("Not Available"),
      velocityUser = if (arg.isVelocityUser) arg.velocityLevel else None,
      isDefault = arg.isDefaultContact))
  }

  implicit class ContactAddressAdapter(val arg: BusinessUserApplicationAddress) extends AnyVal {
    def asDomain = Try(ContactAddress(
      addressType = arg.addressType,
      country = arg.country,
      city = arg.city.sanitize,
      postalCode = arg.postalCode, //objectify
      address = arg.address.sanitize,
      coordinates = if (arg.coordinateX.isEmpty && arg.coordinateY.isEmpty) None
      else Some(AddressCoordinates(arg.coordinateX.get.toDouble, arg.coordinateY.get.toDouble))))
  }
}
