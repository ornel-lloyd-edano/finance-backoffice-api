package tech.pegb.backoffice.domain.customer.dto

import java.time.{LocalDate}

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUserType
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class IndividualUserCriteria(
    userId: Option[UUIDLike] = None,
    msisdnLike: Option[MsisdnLike] = None,
    tier: Option[CustomerTier] = None,
    segment: Option[CustomerSegment] = None,
    subscription: Option[CustomerSubscription] = None,
    status: Option[CustomerStatus] = None,
    individualUserType: Option[IndividualUserType] = None,
    name: Option[NameAttribute] = None,
    fullName: Option[NameAttribute] = None,
    gender: Option[NameAttribute] = None,
    personId: Option[NameAttribute] = None,
    documentNumber: Option[NameAttribute] = None,
    documentType: Option[NameAttribute] = None,
    birthDate: Option[LocalDate] = None,
    birthPlace: Option[NameAttribute] = None,
    nationality: Option[NameAttribute] = None,
    occupation: Option[NameAttribute] = None,
    companyName: Option[NameAttribute] = None,
    employer: Option[NameAttribute] = None,
    createdDateFrom: Option[LocalDate] = None,
    createdDateTo: Option[LocalDate] = None,
    updatedDateFrom: Option[LocalDate] = None,
    updatedDateTo: Option[LocalDate] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
