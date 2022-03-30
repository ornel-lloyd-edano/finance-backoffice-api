package tech.pegb.backoffice.dao.limit.dto

import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.util.UUIDLike
//TODO change isDeleted to deletedAt (isDeleted remains in domain)
case class LimitProfileCriteria(
    uuid: Option[CriteriaField[UUIDLike]] = None,
    limitType: Option[CriteriaField[String]] = None,
    userType: Option[CriteriaField[String]] = None,
    tier: Option[CriteriaField[String]] = None,
    subscription: Option[CriteriaField[String]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    channel: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None,
    instrument: Option[CriteriaField[String]] = None,
    interval: Option[CriteriaField[String]] = None,
    currencyCode: Option[CriteriaField[String]] = None,
    isDeleted: Option[CriteriaField[Boolean]] = None)

