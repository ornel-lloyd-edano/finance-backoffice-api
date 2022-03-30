package tech.pegb.backoffice.dao.commission.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class CommissionProfileCriteria(
    uuid: Option[CriteriaField[String]] = None,
    businessType: Option[CriteriaField[String]] = None,
    tier: Option[CriteriaField[String]] = None,
    subscriptionType: Option[CriteriaField[String]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    currency: Option[CriteriaField[String]] = None,
    channel: Option[CriteriaField[String]] = None,
    instrument: Option[CriteriaField[String]] = None,
    calculationMethod: Option[CriteriaField[String]] = None,
    isDeleted: Option[CriteriaField[Boolean]] = None)
