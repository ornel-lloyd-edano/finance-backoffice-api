package tech.pegb.backoffice.dao.customer.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class VelocityPortalUsersCriteria(
    uuid: Option[CriteriaField[String]] = None,
    userId: Option[CriteriaField[Int]] = None,
    userUUID: Option[CriteriaField[String]] = None,
    name: Option[CriteriaField[String]] = None,
    role: Option[CriteriaField[String]] = None,
    username: Option[CriteriaField[String]] = None,
    email: Option[CriteriaField[String]] = None,
    msisdn: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[_]] = None,
    lastLoginAt: Option[CriteriaField[_]] = None)
