package tech.pegb.backoffice.mapping.dao.domain.limit

import java.util.Currency

import tech.pegb.backoffice.dao.limit.entity.LimitProfile
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.limit.model
import tech.pegb.backoffice.domain.limit.model.{LimitType, TimeIntervals}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}

object Implicits {
  implicit class LimitProfileAdapter(val arg: LimitProfile) extends AnyVal { //TODO wrap in Try and handle elsewhere
    def asDomain: model.LimitProfile = {
      val limitType = LimitType.fromString(arg.limitType)
      val (maxBalanceAmount,
        interval,
        maxIntervalAmount,
        maxAmountPerTransaction,
        minAmountPerTransaction,
        maxCount) = limitType match {
        case LimitType.BalanceBased ⇒ (arg.maxAmount, None, None, None, None, None)
        case _ ⇒ (None, arg.interval.map(TimeIntervals.fromString), arg.maxIntervalAmount, arg.maxAmount, arg.minAmount, arg.maxCount)
      }

      model.LimitProfile(
        id = arg.uuid,
        limitType = limitType,
        userType = UserType(arg.userType),
        tier = CustomerTier(arg.tier),
        subscription = CustomerSubscription(arg.subscription),
        transactionType = arg.transactionType.map(TransactionType),
        channel = arg.channel.map(Channel),
        otherParty = arg.provider,
        instrument = arg.instrument,
        currencyCode = Currency.getInstance(arg.currencyCode),
        createdBy = arg.createdBy,
        createdAt = arg.createdAt,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt,

        maxBalanceAmount = maxBalanceAmount,

        interval = interval,
        maxIntervalAmount = maxIntervalAmount,
        maxAmountPerTransaction = maxAmountPerTransaction,
        minAmountPerTransaction = minAmountPerTransaction,
        maxCount = maxCount)
    }
  }
}
