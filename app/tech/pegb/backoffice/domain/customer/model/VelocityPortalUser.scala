package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerStatus, Msisdn}

case class VelocityPortalUser(
    uuid: UUID,
    name: String,
    middleName: Option[String],
    surname: String,
    fullName: String,
    msisdn: Msisdn,
    email: Email,
    username: String,
    role: String,
    status: CustomerStatus,
    lastLoginAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime])
