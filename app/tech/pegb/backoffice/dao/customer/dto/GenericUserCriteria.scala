package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDate

import tech.pegb.backoffice.dao.model.CriteriaField

case class GenericUserCriteria(
    userUuid: Option[CriteriaField[String]] = None,
    msisdn: Option[CriteriaField[String]] = None,
    tier: Option[CriteriaField[String]] = None,
    segment: Option[CriteriaField[String]] = None,
    subscription: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    customerType: Option[CriteriaField[String]] = None,
    name: Option[CriteriaField[String]] = None,
    fullName: Option[CriteriaField[String]] = None,
    gender: Option[CriteriaField[String]] = None,
    personId: Option[CriteriaField[String]] = None,
    documentNumber: Option[CriteriaField[String]] = None,
    documentType: Option[CriteriaField[String]] = None,
    birthDate: Option[CriteriaField[LocalDate]] = None,
    birthPlace: Option[CriteriaField[String]] = None,
    nationality: Option[CriteriaField[String]] = None,
    occupation: Option[CriteriaField[String]] = None,
    company: Option[CriteriaField[String]] = None,
    employer: Option[CriteriaField[String]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[_]] = None,
    anyName: Option[CriteriaField[String]] = None)
