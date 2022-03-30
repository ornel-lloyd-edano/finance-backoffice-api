package tech.pegb.backoffice.mapping.dao.domain.customer

import java.time.LocalDate
import java.util.{Currency, UUID}

import cats.implicits._
import tech.pegb.backoffice.dao.address
import tech.pegb.backoffice.dao.contacts.entity.Contact
import tech.pegb.backoffice.dao.customer.dto.PaymentOptionDto
import tech.pegb.backoffice.dao.customer.entity
import tech.pegb.backoffice.dao.customer.entity.{BusinessUser, VelocityPortalUser}
import tech.pegb.backoffice.dao.savings.entity.SavingOption
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.model.AddressCoordinates
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessTypes
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes.{BusinessCategory, RegistrationNumber, TaxNumber}
import tech.pegb.backoffice.domain.customer.model
import tech.pegb.backoffice.domain.customer.model.CardApplicationAttributes.{CardApplicationStatus, CardApplicationType, CardPin, CardType}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.{IndividualUser, IndividualUserType}
import tech.pegb.backoffice.domain.customer.model._

import scala.util.Try

object Implicits {

  implicit class IndividualUserAdapter(arg: entity.IndividualUser) {
    def asDomain: Try[IndividualUser] = Try {
      IndividualUser(
        id = UUID.fromString(arg.uuid),
        userName = arg.username.map(LoginUsername),
        password = arg.password,
        tier = arg.tier.map(CustomerTier),
        segment = arg.segment.map(CustomerSegment),
        subscription = arg.subscription.map(CustomerSubscription),
        email = arg.email.map(Email(_)),
        status = CustomerStatus(arg.status),
        msisdn = Msisdn(arg.msisdn),
        individualUserType = arg.`type`.map(IndividualUserType),
        name = arg.name,
        fullName = arg.fullName,
        gender = arg.gender,
        personId = arg.personId,
        documentNumber = arg.documentNumber,
        documentModel = arg.documentModel,
        birthDate = arg.birthDate,
        birthPlace = arg.birthPlace.map(NameAttribute),
        nationality = arg.nationality.map(NameAttribute),
        occupation = arg.occupation.map(NameAttribute),
        companyName = arg.company.map(NameAttribute),
        employer = arg.employer.map(NameAttribute),
        createdAt = arg.createdAt,
        createdBy = Some(arg.createdBy),
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy,
        activatedAt = arg.activatedAt,
        uniqueId = arg.uuid)
    }
  }

  implicit class GenericUserAdapter(arg: entity.GenericUser) {
    def asDomain: Try[GenericUser] = Try {
      GenericUser(
        dbUserId = arg.id,
        id = UUID.fromString(arg.uuid),
        userName = LoginUsername(arg.userName).some,
        password = arg.password,
        tier = arg.tier.map(CustomerTier),
        segment = arg.segment.map(CustomerSegment),
        subscription = arg.subscription.map(CustomerSubscription),
        email = arg.email.map(Email(_)),
        status = arg.status.map(CustomerStatus),
        customerType = arg.customerType.map(CustomerType),
        createdAt = arg.createdAt,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy,
        activatedAt = arg.activatedAt,
        passwordUpdatedAt = arg.passwordUpdatedAt,
        customerName = getCustomerName(arg.customerName, arg.businessUserFields),

        msisdn = arg.individualUserFields.map(i ⇒ Msisdn(i.msisdn)),
        individualUserType = arg.individualUserFields.flatMap(i ⇒ i.`type`.map(IndividualUserType)),
        name = arg.individualUserFields.flatMap(i ⇒ i.name),
        fullName = arg.individualUserFields.flatMap(i ⇒ i.fullName),
        gender = arg.individualUserFields.flatMap(i ⇒ i.gender),
        personId = arg.individualUserFields.flatMap(i ⇒ i.personId),
        documentNumber = arg.individualUserFields.flatMap(i ⇒ i.documentNumber),
        documentType = arg.individualUserFields.flatMap(i ⇒ i.documentType),
        documentModel = arg.individualUserFields.flatMap(i ⇒ i.documentModel),
        birthDate = arg.individualUserFields.flatMap(i ⇒ i.birthDate),
        birthPlace = arg.individualUserFields.flatMap(i ⇒ i.birthPlace.map(NameAttribute)),
        nationality = arg.individualUserFields.flatMap(i ⇒ i.nationality.map(NameAttribute)),
        occupation = arg.individualUserFields.flatMap(i ⇒ i.occupation.map(NameAttribute)),
        companyName = arg.individualUserFields.flatMap(i ⇒ i.company.map(NameAttribute)),
        employer = arg.individualUserFields.flatMap(i ⇒ i.employer.map(NameAttribute)),

        businessName = arg.businessUserFields.map(i ⇒ NameAttribute(i.businessName)),
        brandName = arg.businessUserFields.map(i ⇒ NameAttribute(i.brandName)),
        businessCategory = arg.businessUserFields.map(i ⇒ BusinessCategory(i.businessCategory)),
        businessType = arg.businessUserFields.map(i ⇒ BusinessTypes.fromString(i.businessType)),
        registrationNumber = arg.businessUserFields.map(i ⇒ RegistrationNumber(i.registrationNumber)),
        taxNumber = arg.businessUserFields.flatMap(i ⇒ i.taxNumber.map(TaxNumber(_))),
        registrationDate = arg.businessUserFields.flatMap(_.registrationDate))
    }

    private def getCustomerName(maybeName: Option[String], maybeBusinessUser: Option[BusinessUser]): Option[String] = {
      val maybeBrandName = maybeBusinessUser.map(_.brandName)

      (maybeName, maybeBrandName) match {
        case (Some(name), Some(brandName)) if name == brandName ⇒ Some(name)
        case (Some(name), Some(brandName)) ⇒ Some(s"$name ($brandName)")
        case (None, Some(brandName)) ⇒ Some(brandName)
        case (Some(name), None) ⇒ Some(name)
        case _ ⇒ None
      }
    }
  }

  implicit class CardApplicationAdapter(arg: entity.CardApplication) {
    def asDomain: Try[model.CardApplication] = {
      Try(model.CardApplication(
        applicationId = UUID.fromString(arg.id),
        customerId = UUID.fromString(arg.userId),
        operationType = CardApplicationType(arg.operationType),
        cardType = CardType(arg.cardType),
        nameOnCard = NameAttribute(arg.nameOnCard),
        cardPin = CardPin(arg.cardPin),
        deliveryAddress = Address(arg.deliveryAddress),
        status = CardApplicationStatus(arg.status),
        createdAt = arg.createdAt,
        createdBy = Some(arg.createdBy)))
    }
  }

  implicit class EmployerAdapter(arg: entity.Employer) {
    def asDomain() = Employer(arg.employerName)
  }

  implicit class NationalityAdapter(arg: entity.Nationality) {
    def asDomain() = Nationality(arg.name)
  }

  implicit class OccupationAdapter(arg: entity.Occupation) {
    def asDomain() = Occupation(arg.name)
  }

  implicit class CompanyAdapter(arg: entity.Company) {
    def asDomain() = Company(arg.companyName)
  }

  implicit class PaymentOptionAdapter(val arg: PaymentOptionDto) extends AnyVal {
    def asDomain = PaymentOption(
      id = arg.id,
      customerId = arg.customerId,
      `type` = arg.`type`,
      provider = arg.provider,
      maskedNumber = arg.maskedNumber,
      additionalData = arg.additionalData,
      addedAt = arg.addedAt)
  }

  implicit class SavingOptionAdapter(val arg: SavingOption) {
    def asGenericSavingOption(
      customerId: UUID,
      savingOptionType: SavingOptionType,
      savingGoalName: Option[String] = None,
      goalAmount: Option[BigDecimal] = None,
      reason: Option[String] = None,
      dueDate: Option[LocalDate]): Try[GenericSavingOption] = {
      Try(GenericSavingOption(
        id = UUID.fromString(arg.uuid),
        customerId = customerId,
        savingType = savingOptionType,
        savingGoalName = savingGoalName,
        amount = goalAmount,
        currentAmount = arg.currentAmount,
        currency = Currency.getInstance(arg.currency),
        reason = reason,
        createdAt = arg.createdAt,
        dueDate = dueDate,
        updatedAt = arg.updatedAt))
    }
  }

  implicit class VelocityPortalUserAdapter(val arg: VelocityPortalUser) {
    def asDomain = Try(model.VelocityPortalUser(
      uuid = UUID.fromString(arg.uuid),
      name = arg.name,
      middleName = arg.middleName,
      surname = arg.surname,
      fullName = s"${arg.name} ${arg.middleName.fold(arg.surname)(m ⇒ s"$m ${arg.surname}")}",
      msisdn = Msisdn(arg.msisdn),
      email = Email(arg.email),
      username = arg.username,
      role = arg.role,
      status = CustomerStatus(arg.status),
      lastLoginAt = arg.lastLoginAt,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt))
  }

  implicit class ContactAdapter(val arg: Contact) {
    def asDomain = Try(model.Contact(
      id = arg.id,
      uuid = UUID.fromString(arg.uuid),
      buApplicationId = arg.buApplicationId,
      buApplicationUUID = arg.buApplicationUUID.map(UUID.fromString(_)),
      userId = arg.userId,
      userUUID = arg.userUUID.map(UUID.fromString(_)),
      contactType = ContactTypes.fromString(arg.contactType),
      name = arg.name,
      middleName = arg.middleName,
      surname = arg.surname,
      phoneNumber = Msisdn(arg.phoneNumber),
      email = Email(arg.email),
      idType = arg.idType,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt,
      vpUserId = arg.vpUserId,
      vpUserUUID = arg.vpUserUUID.map(UUID.fromString(_)),
      isActive = arg.isActive))
  }

  implicit class AddressAdapter(val arg: address.entity.Address) {
    def asDomain = Try(model.ContactAddress(
      id = arg.id,
      uuid = UUID.fromString(arg.uuid),
      buApplicationId = arg.buApplicationId,
      buApplicationUuid = arg.buApplicationUuid.map(UUID.fromString(_)),
      userId = arg.userId,
      userUuid = arg.userUuid.map(UUID.fromString(_)),
      addressType = AddressTypes.fromString(arg.addressType),
      countryId = arg.countryId,
      countryName = arg.countryName,
      city = arg.city,
      postalCode = arg.postalCode,
      address = arg.address,
      coordinates = (arg.coordinateX, arg.coordinateY) match {
        case (Some(x), Some(y)) ⇒ AddressCoordinates(x.toDouble, y.toDouble).some
        case _ ⇒ None
      },
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt,
      isActive = arg.isActive))
  }
}
