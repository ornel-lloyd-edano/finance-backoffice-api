package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn

case class ContactToCreate(
    uuid: UUID,
    userUuid: UUID,
    contactType: String,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: Msisdn,
    email: Email,
    idType: String,
    createdBy: String,
    createdAt: LocalDateTime,
    isActive: Boolean)
