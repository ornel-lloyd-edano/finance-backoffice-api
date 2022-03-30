package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDate

import tech.pegb.backoffice.dao.model.CriteriaField

case class IndividualUserCriteria(
    userUuid: Option[CriteriaField[String]] = None,
    msisdn: Option[CriteriaField[String]] = None,
    tier: Option[String] = None,
    segment: Option[String] = None,
    subscription: Option[String] = None,
    status: Option[String] = None,
    individualUserType: Option[String] = None,
    name: Option[String] = None,
    fullName: Option[CriteriaField[String]] = None,
    gender: Option[String] = None,
    personId: Option[String] = None,
    documentNumber: Option[String] = None,
    documentType: Option[String] = None,
    birthDate: Option[LocalDate] = None,
    birthPlace: Option[String] = None,
    nationality: Option[String] = None,
    occupation: Option[String] = None,
    company: Option[String] = None,
    employer: Option[String] = None,
    createdBy: Option[String] = None,
    createdDateFrom: Option[LocalDate] = None,
    createdDateTo: Option[LocalDate] = None,
    updatedBy: Option[String] = None,
    updatedDateFrom: Option[LocalDate] = None,
    updatedDateTo: Option[LocalDate] = None)
