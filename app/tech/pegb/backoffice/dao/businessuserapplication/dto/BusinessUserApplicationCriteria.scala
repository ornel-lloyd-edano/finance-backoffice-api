package tech.pegb.backoffice.dao.businessuserapplication.dto

import java.util.UUID

import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}

case class BusinessUserApplicationCriteria(
    uuid: Option[CriteriaField[String]] = None,
    businessName: Option[CriteriaField[String]] = None,
    brandName: Option[CriteriaField[String]] = None,
    businessCategory: Option[CriteriaField[String]] = None,
    contactsPhoneNumber: Option[CriteriaField[String]] = None,
    contactsEmail: Option[CriteriaField[String]] = None,
    stage: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    userTier: Option[CriteriaField[String]] = None,
    businessType: Option[CriteriaField[String]] = None,
    registrationNumber: Option[CriteriaField[String]] = None,
    taxNumber: Option[CriteriaField[String]] = None,
    registrationDate: Option[CriteriaField[_]] = None,
    submittedBy: Option[CriteriaField[String]] = None,
    submittedAt: Option[CriteriaField[_]] = None,
    checkedBy: Option[CriteriaField[String]] = None,
    checkedAt: Option[CriteriaField[_]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[_]] = None,
    isActive: Option[CriteriaField[Boolean]] = None)

object BusinessUserApplicationCriteria {
  def apply(uuid: UUID) = new BusinessUserApplicationCriteria(uuid = Some(CriteriaField[String]("uuid", uuid.toString, MatchTypes.Exact)))
}
