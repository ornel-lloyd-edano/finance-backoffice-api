package tech.pegb.backoffice.domain.customer.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.util.HasPartialMatch

case class ContactsCriteria(
    uuid: Option[UUID] = None,
    userUuid: Option[UUID] = None,
    buApplicUuid: Option[UUID] = None,
    contactType: Option[String] = None,
    name: Option[String] = None,
    middleName: Option[String] = None,
    surname: Option[String] = None,
    phoneNumber: Option[String] = None,
    email: Option[String] = None,
    idType: Option[String] = None,
    isActive: Option[Boolean] = None,
    vpUserId: Option[Int] = None,
    vpUserUUID: Option[String] = None,
    createdBy: Option[String] = None,
    createdFrom: Option[LocalDateTime] = None,
    createdTo: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None,
    updatedFrom: Option[LocalDateTime] = None,
    updatedTo: Option[LocalDateTime] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
