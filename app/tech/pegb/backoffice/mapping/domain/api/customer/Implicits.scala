package tech.pegb.backoffice.mapping.domain.api.customer

import tech.pegb.backoffice.api.customer.dto
import tech.pegb.backoffice.domain.account.model.{FloatAccountAggregation, Account ⇒ DomainAccount, ExternalAccount ⇒ DomainExternalAccount}
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.{ActivatedBusinessUser ⇒ DomainActivatedBusinessUser, RegisteredButNotActivatedBusinessUser ⇒ DomainRegisteredButNotActivatedBusinessUser}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{ActivationDocumentType ⇒ DomainActivationDocumentType, Address ⇒ DomainAddress, CustomerStatus ⇒ DomainCustomerStatus}
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.{IndividualUser ⇒ DomainIndividualUser}
import tech.pegb.backoffice.api.customer.dto.CustomerAttributes.{ActivationDocumentType, Address, CustomerStatus}
import tech.pegb.backoffice.api.customer.dto._
import tech.pegb.backoffice.domain.customer.model.{Contact, ContactAddress, GenericUser, VelocityPortalUser}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class AddressConverter(val address: DomainAddress) {
    def asApi = Address(underlying = address.underlying)
  }

  implicit class AccountConverter(val account: DomainAccount) {
    def asApi = AccountToRead(
      id = account.id.toString,
      customerId = account.customerId.toString,
      customerName = account.anyCustomerName,
      customerFullName = account.customerName.getOrElse(""),
      msisdn = account.msisdn.map(_.underlying).getOrElse(""),
      number = account.accountNumber.underlying,
      name = account.accountName.underlying,
      `type` = account.accountType.underlying.toLowerCase,
      isMainAccount = account.isMainAccount,
      currency = account.currency.getCurrencyCode,
      balance = account.balance,
      blockedBalance = account.blockedBalance,
      availableBalance = account.availableBalance,
      status = account.accountStatus.underlying.toLowerCase,
      lastTransactionAt = account.lastTransactionAt.map(_.toZonedDateTimeUTC),
      mainType = account.mainType.underlying,
      createdAt = account.createdAt.toZonedDateTimeUTC,
      createdBy = account.createdBy.getOrElse("UNKNOWN"),
      updatedBy = account.updatedBy,
      updatedAt = account.updatedAt.map(_.toZonedDateTimeUTC))
  }

  implicit class AccountAggregationConverter(val accountAgg: FloatAccountAggregation) {
    def asApi = FloatAccountAggregationToRead(
      id = accountAgg.accountUuid.toString,
      userId = accountAgg.userUuid.toString,
      userName = accountAgg.userName,
      accountNumber = accountAgg.accountNumber.underlying.toLowerCase,
      `type` = accountAgg.accountType.underlying.toLowerCase,
      mainType = accountAgg.accountMainType.underlying.toLowerCase,
      currency = accountAgg.currency.getDisplayName,
      internalBalance = accountAgg.internalBalance,
      externalBalance = accountAgg.externalBalance,
      inflow = accountAgg.inflow,
      outflow = accountAgg.outflow,
      net = accountAgg.net,
      createdAt = accountAgg.createdAt.toZonedDateTimeUTC,
      updatedAt = accountAgg.updatedAt.map(_.toZonedDateTimeUTC))
  }

  implicit class ActivatedBusinessUserConverter(val activatedBusinessUser: DomainActivatedBusinessUser) {
    def asApi = ActivatedBusinessUserToRead(
      id = activatedBusinessUser.id,
      username = Option(activatedBusinessUser.username.underlying),
      password = Option(activatedBusinessUser.password),
      tier = activatedBusinessUser.tier.underlying,
      segment = activatedBusinessUser.segment.map(_.underlying),
      subscription = activatedBusinessUser.subscription.underlying,
      emails = activatedBusinessUser.emails.map(_.value),
      status = activatedBusinessUser.status.underlying,
      name = activatedBusinessUser.name.underlying,
      addresses = activatedBusinessUser.addresses.map(_.asApi),
      phoneNumbers = activatedBusinessUser.phoneNumbers.map(_.underlying),
      activationRequirements = activatedBusinessUser.activationRequirements.map(_.identifier),

      accounts = activatedBusinessUser.accounts.map(_.asApi),
      activatedAt = activatedBusinessUser.activatedAt,
      passwordUpdatedAt = activatedBusinessUser.passwordUpdatedAt,
      createdAt = activatedBusinessUser.createdAt,
      createdBy = activatedBusinessUser.createdBy.getOrElse("UNKNOWN"),
      updatedAt = activatedBusinessUser.updatedAt,
      updatedBy = activatedBusinessUser.updatedBy)
  }

  implicit class RegisteredButNotActivatedBusinessUserConverter(val registeredButNotActivatedBusinessUser: DomainRegisteredButNotActivatedBusinessUser) {
    def asApi = RegisteredButNotActivatedBusinessUserToRead(
      id = registeredButNotActivatedBusinessUser.id,
      username = registeredButNotActivatedBusinessUser.username.map(_.underlying).getOrElse(null),
      email = registeredButNotActivatedBusinessUser.email.value,
      name = registeredButNotActivatedBusinessUser.name.underlying,
      accounts = registeredButNotActivatedBusinessUser.accounts.map(_.asApi),
      createdAt = registeredButNotActivatedBusinessUser.createdAt,
      createdBy = registeredButNotActivatedBusinessUser.createdBy.getOrElse("UNKNOWN"))
  }

  implicit class CustomerStatusConverter(val customerStatus: DomainCustomerStatus) {
    def asApi = CustomerStatus(
      status = customerStatus.underlying)
  }

  implicit class ActivationDocumentTypeConverter(val activationDocumentType: DomainActivationDocumentType) {
    def asApi = ActivationDocumentType(
      `type` = activationDocumentType.underlying)
  }

  implicit class IndividualUserConverter(val individualUser: DomainIndividualUser) extends AnyVal {
    def asApi = IndividualUserResponse(
      id = individualUser.id,
      username = individualUser.userName.map(_.underlying),
      tier = individualUser.tier.map(_.underlying),
      segment = individualUser.segment.map(_.underlying),
      subscription = individualUser.subscription.map(_.underlying),
      email = individualUser.email.map(_.value),
      status = Option(individualUser.status.underlying),
      msisdn = individualUser.msisdn.underlying,
      individualUserType = individualUser.individualUserType.map(_.underlying),
      alias = individualUser.name.getOrElse(""),
      fullName = individualUser.fullName,
      gender = individualUser.gender,
      personId = individualUser.personId,
      documentNumber = individualUser.documentNumber,
      documentType = None,
      documentModel = individualUser.documentModel,
      birthDate = individualUser.birthDate,
      birthPlace = individualUser.birthPlace.map(_.underlying),
      nationality = individualUser.nationality.map(_.underlying),
      occupation = individualUser.occupation.map(_.underlying),
      companyName = individualUser.companyName.map(_.underlying),
      employer = individualUser.employer.map(_.underlying),
      createdAt = individualUser.createdAt.toZonedDateTimeUTC,
      createdBy = individualUser.createdBy.getOrElse("UNKNOWN"),
      updatedAt = individualUser.updatedAt.map(_.toZonedDateTimeUTC),
      updatedBy = individualUser.updatedBy,
      activatedAt = individualUser.activatedAt.map(_.toZonedDateTimeUTC))
  }

  implicit class GenericUserConverter(val arg: GenericUser) extends AnyVal {
    def asApi = dto.GenericUserToRead(
      id = arg.id,
      username = arg.userName.map(_.underlying),
      tier = arg.tier.map(_.underlying),
      segment = arg.segment.map(_.underlying),
      subscription = arg.subscription.map(_.underlying),
      email = arg.email.map(_.value),
      status = arg.status.map(_.underlying),
      customerType = arg.customerType.map(_.underlying),
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      createdBy = arg.createdBy,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
      updatedBy = arg.updatedBy,
      activatedAt = arg.activatedAt.map(_.toZonedDateTimeUTC),
      passwordUpdatedAt = arg.activatedAt.map(_.toZonedDateTimeUTC),
      customerName = arg.customerName,

      msisdn = arg.msisdn.map(_.underlying),
      individualUserType = arg.individualUserType.map(_.underlying),
      alias = arg.name,
      fullName = arg.fullName,
      gender = arg.gender,
      personId = arg.personId,
      documentNumber = arg.documentNumber,
      documentType = arg.documentType,
      documentModel = arg.documentModel,
      birthDate = arg.birthDate,
      birthPlace = arg.birthPlace.map(_.underlying),
      nationality = arg.nationality.map(_.underlying),
      occupation = arg.occupation.map(_.underlying),
      companyName = arg.companyName.map(_.underlying),
      employer = arg.employer.map(_.underlying),

      businessName = arg.businessName.map(_.underlying),
      brandName = arg.brandName.map(_.underlying),
      businessType = arg.businessType.map(_.toString),
      businessCategory = arg.businessCategory.map(_.underlying),
      registrationNumber = arg.registrationNumber.map(_.underlying),
      taxNumber = arg.taxNumber.map(_.underlying),
      registrationDate = arg.registrationDate)
  }

  implicit class VelocityPortalUserConverter(val arg: VelocityPortalUser) extends AnyVal {
    def asApi = VelocityPortalUserToRead(
      id = arg.uuid,
      name = arg.name,
      middleName = arg.middleName,
      surname = arg.surname,
      fullName = arg.fullName,
      msisdn = arg.msisdn.underlying,
      email = arg.email.value,
      username = arg.username,
      role = arg.role,
      status = arg.status.underlying,
      lastLoginAt = arg.lastLoginAt.map(_.toZonedDateTimeUTC),
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      createdBy = arg.createdBy,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
      updatedBy = arg.updatedBy)
  }

  implicit class ContactConverter(val arg: Contact) extends AnyVal {
    def asApi = dto.ContactToRead(
      id = arg.uuid,
      contactType = arg.contactType.toString,
      name = arg.name,
      middleName = arg.middleName,
      surname = arg.surname,
      phoneNumber = arg.phoneNumber.underlying,
      email = arg.email.value,
      idType = arg.idType,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      createdBy = arg.createdBy,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
      updatedBy = arg.updatedBy)
  }

  implicit class ContactAddressConverter(val arg: ContactAddress) extends AnyVal {
    def asApi = dto.ContactAddressToRead(
      id = arg.uuid,
      addressType = arg.addressType.toString,
      countryName = arg.countryName,
      city = arg.city,
      postalCode = arg.postalCode,
      address = arg.address,
      coordinateX = arg.coordinates.map(p ⇒ BigDecimal(p.x)),
      coordinateY = arg.coordinates.map(p ⇒ BigDecimal(p.y)),
      createdBy = arg.createdBy,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
      isActive = arg.isActive)
  }

  implicit class ExternalAccountApiAdapter(val arg: DomainExternalAccount) extends AnyVal {
    def asApi = dto.ExternalAccountToRead(
      id = arg.id,
      customerId = arg.customerId,
      provider = arg.externalProvider,
      accountHolder = arg.externalAccountHolder,
      accountNumber = arg.externalAccountNumber,
      currency = arg.currency,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }
}
