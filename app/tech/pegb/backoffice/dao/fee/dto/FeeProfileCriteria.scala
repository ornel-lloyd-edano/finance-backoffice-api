package tech.pegb.backoffice.dao.fee.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class FeeProfileCriteria(
    id: Option[CriteriaField[String]] = None,
    feeType: Option[CriteriaField[String]] = None,
    userType: Option[CriteriaField[String]] = None,
    tier: Option[CriteriaField[String]] = None,
    subscriptionType: Option[CriteriaField[String]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    channel: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None,
    instrument: Option[CriteriaField[String]] = None,
    calculationMethod: Option[CriteriaField[String]] = None,
    currencyCode: Option[CriteriaField[String]] = None,
    feeMethod: Option[CriteriaField[String]] = None,
    taxIncluded: Option[CriteriaField[String]] = None,
    isDeleted: Option[CriteriaField[Boolean]] = None)
