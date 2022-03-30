package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class BackOfficeUserCriteria(
    id: Option[CriteriaField[String]] = None,
    userName: Option[CriteriaField[String]] = None,
    password: Option[CriteriaField[String]] = None,
    roleId: Option[CriteriaField[String]] = None,
    roleName: Option[CriteriaField[String]] = None,
    roleLevel: Option[CriteriaField[Int]] = None,
    businessUnitName: Option[CriteriaField[String]] = None,
    businessUnitId: Option[CriteriaField[String]] = None,
    scopeId: Option[CriteriaField[String]] = None,
    scopeName: Option[CriteriaField[String]] = None,
    email: Option[CriteriaField[String]] = None,
    phoneNumber: Option[CriteriaField[String]] = None,
    firstName: Option[CriteriaField[String]] = None,
    middleName: Option[CriteriaField[String]] = None,
    lastName: Option[CriteriaField[String]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    isActive: Option[CriteriaField[Int]] = None) {

}
