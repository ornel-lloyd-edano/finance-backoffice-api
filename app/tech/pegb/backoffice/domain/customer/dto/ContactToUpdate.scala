package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn

case class ContactToUpdate(
    contactType: Option[String] = None,
    name: Option[String] = None,
    middleName: Option[String] = None,
    surname: Option[String] = None,
    phoneNumber: Option[Msisdn] = None,
    email: Option[Email] = None,
    idType: Option[String] = None,
    isActive: Option[Boolean] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])
