package tech.pegb.backoffice.domain.limit.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.WithSmartString

case class LimitProfile(
    id: UUID,
    limitType: LimitType,
    userType: UserType,
    tier: CustomerTier,
    subscription: CustomerSubscription,
    transactionType: Option[TransactionType],
    channel: Option[Channel],
    otherParty: Option[String],
    instrument: Option[String],
    currencyCode: Currency,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],

    //balance_based
    maxBalanceAmount: Option[BigDecimal] = None,

    //transaction_based
    interval: Option[TimeInterval] = None,
    maxIntervalAmount: Option[BigDecimal] = None,
    maxAmountPerTransaction: Option[BigDecimal] = None,
    minAmountPerTransaction: Option[BigDecimal] = None,
    maxCount: Option[Int] = None) extends WithSmartString {

  import LimitProfile._

  assertLimitProfileAmounts(maxIntervalAmount, maxAmountPerTransaction, minAmountPerTransaction, maxCount)
  assertLimitProfileRequiredFields(
    limitType = limitType,
    interval = interval,
    maxIntervalAmount = maxIntervalAmount,
    maxAmount = maxAmountPerTransaction,
    minAmount = minAmountPerTransaction,
    maxCount = maxCount,
    maxBalance = maxBalanceAmount)
}

object LimitProfile {
  private val zero = BigDecimal(0)

  def assertLimitProfileAmounts(
    maxIntervalAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minAmount: Option[BigDecimal],
    maxCount: Option[Int]): Unit = {
    maxIntervalAmount.foreach { max ⇒
      assert(max >= zero, "maximum interval amount can not be a negative value")
      assert(max.scale <= 2, "maximum interval amount can not have more than 2 decimal digits")
    }
    minAmount.foreach { min ⇒
      assert(min >= zero, "minumum amount can not be a negative value")
      assert(min.scale <= 2, "minumum amount can not have more than 2 decimal digits")
    }
    maxAmount.foreach { max ⇒
      assert(max >= zero, "maximum amount can not be a negative value")
      assert(max.scale <= 2, "maximum amount can not have more than 2 decimal digits")
      minAmount.foreach(min ⇒ assert(max >= min, "maximum amount can not be less than minimum amount"))
    }
    maxCount.foreach(max ⇒ assert(max >= 0, "maximum count can not be a negative value"))
  }

  def assertLimitProfileRequiredFields(
    limitType: LimitType,
    interval: Option[_],
    maxIntervalAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minAmount: Option[BigDecimal],
    maxCount: Option[Int],
    maxBalance: Option[BigDecimal]): Unit = {
    limitType match {
      case LimitType.TransactionBased ⇒
        assert(maxBalance.isEmpty, s"Max balance have to be left empty for $limitType limit type")
        assert(interval.isDefined, s"Interval have to be specified for $limitType limit type")
        assert(
          maxIntervalAmount.isDefined || maxCount.isDefined || minAmount.isDefined || maxAmount.isDefined,
          s"At least one of: (max interval amount, max count, min amount, max amount) have to be specified for $limitType limit type")
      case LimitType.BalanceBased ⇒
        assert(maxBalance.isDefined, s"Max balance have to be specified for $limitType limit type")
        assert(interval.isEmpty, s"Interval have to be left empty for $limitType limit type")
        assert(maxIntervalAmount.isEmpty, s"Max interval amount have to be left empty for $limitType limit type")
        assert(minAmount.isEmpty, s"Min amount have to be left empty for $limitType limit type")
        assert(maxAmount.isEmpty, s"Max amount have to be left empty for $limitType limit type")
        assert(maxCount.isEmpty, s"Max count have to be left empty for $limitType limit type")
    }
  }
}
