package tech.pegb.backoffice.domain.limit.dto

import java.time.LocalDateTime
import java.util.Currency

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.limit.model.{LimitProfile, LimitType, TimeIntervalWrapper}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}

case class LimitProfileToCreate(
    limitType: LimitType,
    userType: UserType,
    tier: CustomerTier,
    subscription: CustomerSubscription,
    transactionType: Option[TransactionType],
    channel: Option[Channel],
    otherParty: Option[String],
    instrument: Option[String],
    interval: Option[TimeIntervalWrapper],
    maxIntervalAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minAmount: Option[BigDecimal],
    maxCount: Option[Int],
    maxBalance: Option[BigDecimal],
    currencyCode: Currency,
    createdBy: String,
    createdAt: LocalDateTime) {

  LimitProfile.assertLimitProfileAmounts(
    maxIntervalAmount = maxIntervalAmount,
    maxAmount = maxAmount,
    minAmount = minAmount,
    maxCount = maxCount)
  LimitProfile.assertLimitProfileRequiredFields(
    limitType = limitType,
    interval = interval,
    maxIntervalAmount = maxIntervalAmount,
    maxAmount = maxAmount,
    minAmount = minAmount,
    maxCount = maxCount,
    maxBalance = maxBalance)
}

object LimitProfileToCreate {

  lazy val empty = LimitProfileToCreate(
    limitType = LimitType.BalanceBased,
    userType = UserType("individual"),
    tier = CustomerTier("tier_one"),
    subscription = CustomerSubscription("standard"),
    transactionType = Some(TransactionType("top-up")),
    channel = None,
    otherParty = None,
    instrument = None,
    interval = None,
    maxIntervalAmount = None,
    maxAmount = None,
    minAmount = None,
    maxCount = None,
    maxBalance = Some(BigDecimal(Integer.MAX_VALUE)),
    currencyCode = Currency.getInstance("KES"),
    createdBy = "default",
    createdAt = LocalDateTime.now())
}
