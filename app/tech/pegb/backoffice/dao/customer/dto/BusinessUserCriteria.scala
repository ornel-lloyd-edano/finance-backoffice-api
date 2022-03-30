package tech.pegb.backoffice.dao.customer.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class BusinessUserCriteria(
    uuid: Option[CriteriaField[String]] = None,
    businessName: Option[CriteriaField[String]] = None,
    brandName: Option[CriteriaField[String]] = None,
    businessCategory: Option[CriteriaField[String]] = None,
    businessType: Option[CriteriaField[String]] = None,
    registrationNumber: Option[CriteriaField[String]] = None,
    taxNumber: Option[CriteriaField[String]] = None,
    registrationDate: Option[CriteriaField[_]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[_]] = None)

