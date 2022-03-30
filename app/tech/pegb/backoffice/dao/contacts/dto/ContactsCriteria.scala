package tech.pegb.backoffice.dao.contacts.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class ContactsCriteria(
    uuid: Option[CriteriaField[String]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    buApplicUuid: Option[CriteriaField[String]] = None,
    contactType: Option[CriteriaField[String]] = None,
    name: Option[CriteriaField[String]] = None,
    middleName: Option[CriteriaField[String]] = None,
    surname: Option[CriteriaField[String]] = None,
    phoneNumber: Option[CriteriaField[String]] = None,
    email: Option[CriteriaField[String]] = None,
    idType: Option[CriteriaField[String]] = None,
    isActive: Option[CriteriaField[Boolean]] = None,
    vpUserId: Option[CriteriaField[Int]] = None,
    vpUserUUID: Option[CriteriaField[String]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None) {

}
