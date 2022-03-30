package tech.pegb.backoffice.domain.limit.dto

import java.util.Currency

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.limit.model.{LimitType, TimeInterval}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class LimitProfileCriteria(
    uuid: Option[UUIDLike] = None,
    limitType: Option[LimitType] = None,
    userType: Option[UserType] = None,
    tier: Option[CustomerTier] = None,
    subscription: Option[CustomerSubscription] = None,
    transactionType: Option[TransactionType] = None,
    channel: Option[Channel] = None,
    otherParty: Option[String] = None,
    instrument: Option[String] = None,
    interval: Option[TimeInterval] = None,
    currencyCode: Option[Currency] = None,
    isDeleted: Option[Boolean] = Option(false),
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
