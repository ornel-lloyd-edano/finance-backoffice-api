package tech.pegb.backoffice.mapping.domain.dao.businessuserapplication

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.businessuserapplication.dto._
import tech.pegb.backoffice.dao.customer.dto.BusinessUserCriteria
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.{Stages, Status}
import tech.pegb.backoffice.domain.businessuserapplication.dto
import tech.pegb.backoffice.domain.businessuserapplication.dto.{BusinessUserApplicationToCreate, BusinessUserApplicationToUpdateStage, BusinessUserApplicationToUpdateStatus}
import tech.pegb.backoffice.domain.businessuserapplication.model.{ContactAddress, ContactPerson}
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes.{AccountConfig, ExternalAccount, TransactionConfig}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{DateRangeCriteriaWrapper, DateTimeRangeCriteriaWrapper}

import scala.util.Try

object Implicits {

  implicit class PrimaryContactsAdapter(val arg: ContactPerson) extends AnyVal {
    def asDao(internalApplicId: Int, createdBy: String, createdAt: LocalDateTime) = BUApplicPrimaryContactToInsert(
      applicationId = internalApplicId,
      contactType = arg.contactType,
      name = arg.name,
      middleName = arg.middleName,
      surname = arg.surname,
      phoneNumber = arg.phoneNumber.map(_.underlying).getOrElse(""),
      email = arg.email.map(_.value).getOrElse(""),
      idType = arg.idType,
      isVelocityUser = arg.velocityUser.isDefined,
      velocityLevel = arg.velocityUser,
      isDefaultContact = arg.isDefault,
      createdBy = createdBy,
      createdAt = createdAt)
  }

  implicit class PrimaryAddressesAdapter(val arg: ContactAddress) extends AnyVal {
    def asDao(internalApplicId: Int, countryId: Int, createdBy: String, createdAt: LocalDateTime) =
      BUApplicPrimaryAddressToInsert(
        applicationId = internalApplicId,
        addressType = arg.addressType,
        countryId = countryId,
        city = arg.city,
        postalCode = arg.postalCode,
        address = Some(arg.address),
        coordinateX = arg.coordinates.map(_.x),
        coordinateY = arg.coordinates.map(_.y),
        createdBy = createdBy,
        createdAt = createdAt)
  }

  implicit class BusinessUserApplicationToCreateAdapter(val arg: BusinessUserApplicationToCreate) extends AnyVal {
    def asDao = BusinessUserApplicationToInsert(
      uuid = arg.uuid.toString,
      businessName = arg.businessName.underlying,
      brandName = arg.brandName.underlying,
      businessCategory = arg.businessCategory.underlying,
      stage = Stages.Identity,
      status = Status.Ongoing,
      userTier = arg.userTier.toString,
      businessType = arg.businessType.toString,
      registrationNumber = arg.registrationNumber.underlying,
      taxNumber = arg.taxNumber.map(_.underlying),
      registrationDate = arg.registrationDate,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt)

    def asUpdateDao(lastUpdatedAt: Option[LocalDateTime]) = BusinessUserApplicationToUpdate(
      businessName = arg.businessName.underlying.some,
      brandName = arg.brandName.underlying.some,
      businessCategory = arg.businessCategory.underlying.some,
      stage = Stages.Identity.some,
      status = Status.Ongoing.some,
      userTier = arg.userTier.toString.some,
      businessType = arg.businessType.toString.some,
      registrationNumber = arg.registrationNumber.underlying.some,
      taxNumber = arg.taxNumber.map(_.underlying),
      registrationDate = arg.registrationDate,
      updatedBy = arg.createdBy,
      updatedAt = arg.createdAt,
      lastUpdatedAt = lastUpdatedAt)

    def asBusinessUserCriteriaDao = BusinessUserCriteria(
      businessName = CriteriaField("business_name", arg.businessName.underlying).some,
      brandName = CriteriaField("brand_name", arg.brandName.underlying).some,
      registrationNumber = CriteriaField("registration_number", arg.registrationNumber.underlying).some)

    def asApplicationCriteriaDao(butNotThisId: Option[UUID] = None) = BusinessUserApplicationCriteria(
      uuid = butNotThisId.map(uuid ⇒ CriteriaField("uuid", uuid.toString, MatchTypes.NotSame)),
      businessName = CriteriaField("business_name", arg.businessName.underlying).some,
      brandName = CriteriaField("brand_name", arg.brandName.underlying).some,
      registrationNumber = CriteriaField("registration_number", arg.registrationNumber.underlying).some,
      isActive = CriteriaField("", true).some)
  }

  implicit class TransactionConfigToInsertAdapter(val arg: TransactionConfig) extends AnyVal {
    def asDao(currencyMap: Map[String, Int]) = Try {
      TransactionConfigToInsert(arg.transactionType.underlying, currencyMap.get(arg.currency.getCurrencyCode).get)
    }
  }

  implicit class AccountConfigToInsertAdapter(val arg: AccountConfig) extends AnyVal {
    def asDao(currencyMap: Map[String, Int]) = Try {
      AccountConfigToInsert(
        arg.accountType.underlying,
        arg.accountName.underlying,
        currencyMap.get(arg.currency.getCurrencyCode).get,
        arg.isDefault.toInt)
    }
  }

  implicit class ExternalAccountToInsertAdapter(val arg: ExternalAccount) extends AnyVal {
    def asDao(currencyMap: Map[String, Int]) = Try {
      ExternalAccountToInsert(
        arg.provider.underlying,
        arg.accountNumber.underlying,
        arg.accountHolder.underlying,
        currencyMap.get(arg.currency.getCurrencyCode).get)
    }
  }

  implicit class BusinessUserApplicationCriteriaAdapter(val arg: dto.BusinessUserApplicationCriteria) extends AnyVal {
    def asDao = { //TODO find a way so we dont repeatedly pass arg.partialMatchFields
      BusinessUserApplicationCriteria(
        uuid = arg.uuid.map(x ⇒ CriteriaField("uuid", x.underlying, arg.partialMatchFields)),
        businessName = arg.businessName.map(x ⇒ CriteriaField("business_name", x.underlying, arg.partialMatchFields)),
        brandName = arg.brandName.map(x ⇒ CriteriaField("brand_name", x.underlying, arg.partialMatchFields)),
        businessCategory = arg.businessCategory.map(x ⇒ CriteriaField("business_category", x.underlying, arg.partialMatchFields)),
        contactsPhoneNumber = arg.contactPersonsPhoneNumber.map(x ⇒ CriteriaField("phone_number", x, arg.partialMatchFields)),
        contactsEmail = arg.contactPersonsEmail.map(x ⇒ CriteriaField("email", x, arg.partialMatchFields)),
        stage = arg.stage.map(x ⇒ CriteriaField("stage", x.underlying, arg.partialMatchFields)),
        status = arg.status.map(x ⇒ CriteriaField("status", x.underlying, arg.partialMatchFields)),
        userTier = arg.userTier.map(x ⇒ CriteriaField("user_tier", x.underlying, arg.partialMatchFields)),
        businessType = arg.businessType.map(x ⇒ CriteriaField("business_type", x.toString, arg.partialMatchFields)),
        registrationNumber = arg.registrationNumber.map(x ⇒ CriteriaField("registration_number", x.underlying, arg.partialMatchFields)),
        taxNumber = arg.taxNumber.map(x ⇒ CriteriaField("tax_number", x.underlying, arg.partialMatchFields)),
        registrationDate = DateRangeCriteriaWrapper(arg.registrationDateFrom, arg.registrationDateTo, "registration_date").toCriteriaOption,
        submittedBy = arg.submittedBy.map(x ⇒ CriteriaField("submitted_by", x, arg.partialMatchFields)),
        submittedAt = DateTimeRangeCriteriaWrapper(arg.submittedAtFrom, arg.submittedAtTo, "registration_date").toCriteriaOption,
        checkedBy = arg.checkedBy.map(x ⇒ CriteriaField("checked_by", x, arg.partialMatchFields)),
        checkedAt = DateTimeRangeCriteriaWrapper(arg.checkedAtFrom, arg.checkedAtTo, "checked_at").toCriteriaOption,
        createdBy = arg.createdBy.map(x ⇒ CriteriaField("created_by", x, arg.partialMatchFields)),
        createdAt = DateTimeRangeCriteriaWrapper(arg.createdAtFrom, arg.createdAtTo, "created_at").toCriteriaOption,
        updatedBy = arg.updatedBy.map(x ⇒ CriteriaField("updated_by", x, arg.partialMatchFields)),
        updatedAt = DateTimeRangeCriteriaWrapper(arg.updatedAtFrom, arg.updatedAtTo, "updated_at").toCriteriaOption,
        isActive = arg.isActive.map(x ⇒ CriteriaField("", x)))
    }
  }

  implicit class BusinessUserApplicationToUpdateStageAdapter(val arg: BusinessUserApplicationToUpdateStage) extends AnyVal {
    def asDao(lastUpdatedAt: Option[LocalDateTime] = None) = BusinessUserApplicationToUpdate(
      stage = Some(arg.stage), updatedAt = arg.updatedAt, updatedBy = arg.updatedBy, lastUpdatedAt = lastUpdatedAt)
  }

  implicit class BusinessUserApplicationToUpdateStatusAdapter(val arg: BusinessUserApplicationToUpdateStatus) extends AnyVal {
    def asDao(lastUpdatedAt: Option[LocalDateTime] = None) = BusinessUserApplicationToUpdate(
      status = Some(arg.status), explanation = arg.explanation,
      submittedAt = arg.submittedAt, submittedBy = arg.submittedBy,
      checkedAt = arg.checkedAt, checkedBy = arg.checkedBy,
      updatedAt = arg.updatedAt, updatedBy = arg.updatedBy, lastUpdatedAt = lastUpdatedAt)
  }

}
