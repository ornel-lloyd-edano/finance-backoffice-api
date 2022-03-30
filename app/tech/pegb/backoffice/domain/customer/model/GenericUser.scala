package tech.pegb.backoffice.domain.customer.model

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessType
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes.{BusinessCategory, RegistrationNumber, TaxNumber}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUserType

case class GenericUser(
    //user fields
    dbUserId: Int,
    id: UUID,
    userName: Option[LoginUsername],
    password: Option[String],
    tier: Option[CustomerTier],
    segment: Option[CustomerSegment],
    subscription: Option[CustomerSubscription],
    email: Option[Email],
    status: Option[CustomerStatus],
    customerType: Option[CustomerType],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    activatedAt: Option[LocalDateTime],
    passwordUpdatedAt: Option[LocalDateTime],
    customerName: Option[String],

    //Individual User fields
    msisdn: Option[Msisdn],
    individualUserType: Option[IndividualUserType],
    name: Option[String],
    fullName: Option[String],
    gender: Option[String],
    personId: Option[String],
    documentNumber: Option[String],
    documentType: Option[String],
    documentModel: Option[String],
    birthDate: Option[LocalDate],
    birthPlace: Option[NameAttribute],
    nationality: Option[NameAttribute],
    occupation: Option[NameAttribute],
    companyName: Option[NameAttribute],
    employer: Option[NameAttribute],

    //Business User fields
    businessName: Option[NameAttribute],
    brandName: Option[NameAttribute],
    businessCategory: Option[BusinessCategory],
    businessType: Option[BusinessType],
    registrationNumber: Option[RegistrationNumber],
    taxNumber: Option[TaxNumber],
    registrationDate: Option[LocalDate])
