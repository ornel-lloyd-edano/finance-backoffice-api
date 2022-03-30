package tech.pegb.backoffice.dao.fee.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class ThirdPartyFeeProfileCriteria(
    id: Option[CriteriaField[String]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    currencyCode: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None,
    isActive: Option[CriteriaField[Boolean]] = None)
