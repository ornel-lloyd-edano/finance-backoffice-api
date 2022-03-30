package tech.pegb.backoffice.domain.customer.dto

import java.time.{LocalDate}

import tech.pegb.backoffice.domain.auth.model.{Email}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.IndividualUserType

case class IndividualUserToUpdate(
    userName: Option[LoginUsername] = None,
    password: Option[String] = None,
    tier: Option[CustomerTier] = None,
    segment: Option[CustomerSegment] = None,
    subscription: Option[CustomerSubscription] = None,
    email: Option[Email] = None,
    status: Option[CustomerStatus] = None,
    msisdn: Option[Msisdn] = None,
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
    employer: Option[NameAttribute] = None)
