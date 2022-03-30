package tech.pegb.backoffice.domain.businessuserapplication.dto

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessType
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerTier, NameAttribute}
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class BusinessUserApplicationCriteria(
    uuid: Option[UUIDLike] = None,
    businessName: Option[NameAttribute] = None,
    brandName: Option[NameAttribute] = None,
    businessCategory: Option[BusinessCategory] = None,
    stage: Option[ApplicationStage] = None,
    status: Option[ApplicationStatus] = None,
    userTier: Option[CustomerTier] = None,
    businessType: Option[BusinessType] = None,
    registrationNumber: Option[RegistrationNumber] = None,
    taxNumber: Option[TaxNumber] = None,
    registrationDateFrom: Option[LocalDate] = None,
    registrationDateTo: Option[LocalDate] = None,
    submittedBy: Option[String] = None,
    submittedAtFrom: Option[LocalDateTime] = None,
    submittedAtTo: Option[LocalDateTime] = None,
    submittedBusinessUnit: Option[String] = None,
    submitedRole: Option[Int] = None,
    checkedBy: Option[String] = None,
    checkedAtFrom: Option[LocalDateTime] = None,
    checkedAtTo: Option[LocalDateTime] = None,
    createdBy: Option[String] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None,
    updatedAtFrom: Option[LocalDateTime] = None,
    updatedAtTo: Option[LocalDateTime] = None,
    isActive: Option[Boolean] = None,
    contactPersonsPhoneNumber: Option[String] = None,
    contactPersonsEmail: Option[String] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
