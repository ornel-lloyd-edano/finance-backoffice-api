package tech.pegb.backoffice.dao.customer.dto

import java.time.{LocalDate, LocalDateTime}

case class IndividualUserToUpdate(
    `type`: Option[String],
    msisdn: Option[String],
    name: Option[String],
    fullName: Option[String],
    gender: Option[String],
    personId: Option[String],
    documentNumber: Option[String],
    documentType: Option[String],
    company: Option[String],
    birthDate: Option[LocalDate],
    birthPlace: Option[String],
    nationality: Option[String],
    occupation: Option[String],
    employer: Option[String],
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])
