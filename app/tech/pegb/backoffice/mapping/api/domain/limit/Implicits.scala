package tech.pegb.backoffice.mapping.api.domain.limit

import java.time.LocalDateTime
import java.util.Currency

import tech.pegb.backoffice.api.limit.dto.{LimitProfileToCreate, LimitProfileToUpdate}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{CustomerSubscription, CustomerTier}
import tech.pegb.backoffice.domain.customer.model.UserType
import tech.pegb.backoffice.domain.limit.dto.{LimitProfileCriteria, LimitProfileToUpdate ⇒ DomainLimitUpdate}
import tech.pegb.backoffice.domain.limit.model.{LimitType, TimeIntervalWrapper, TimeIntervals}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object Implicits {

  import tech.pegb.backoffice.domain.limit.dto.{LimitProfileToCreate ⇒ LimitProfile}

  implicit class LimitToCreateAdapter(val arg: LimitProfileToCreate) extends AnyVal {
    def asDomain(createdBy: String, createdAt: LocalDateTime): Try[LimitProfile] =
      Try(LimitProfile(
        limitType = LimitType.fromString(arg.limitType),
        userType = UserType(arg.userType),
        tier = CustomerTier(arg.tier),
        subscription = CustomerSubscription(arg.subscription),
        transactionType = arg.transactionType.map(TransactionType(_)),
        channel = arg.channel.map(Channel(_)),
        otherParty = arg.otherParty,
        instrument = arg.instrument,
        interval = arg.interval.map(TimeIntervalWrapper(_)),
        maxIntervalAmount = arg.maxAmountPerInterval,
        maxAmount = arg.maxAmountPerTxn,
        minAmount = arg.minAmountPerTxn,
        maxCount = arg.maxCountPerInterval,
        maxBalance = arg.maxBalanceAmount,
        currencyCode = Currency.getInstance(arg.currencyCode),
        createdBy = createdBy,
        createdAt = createdAt))
  }

  implicit class LimitCriteriaAdapter(arg: (Option[UUIDLike], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Set[String])) {

    def asDomain: Try[LimitProfileCriteria] =
      Try(LimitProfileCriteria(
        uuid = arg._1,
        limitType = arg._2.map(LimitType(_)),
        userType = arg._3.map(UserType),
        tier = arg._4.map(CustomerTier),
        subscription = arg._5.map(CustomerSubscription),
        transactionType = arg._6.map(TransactionType),
        channel = arg._7.map(Channel),
        otherParty = arg._8.map(_.sanitize),
        instrument = arg._9.map(_.sanitize),
        interval = arg._10.map(TimeIntervals(_)),
        currencyCode = arg._11.map(value ⇒ Currency.getInstance(value)),
        partialMatchFields = arg._12))

  }

  implicit class LimitToUpdateAdapter(val arg: LimitProfileToUpdate) extends AnyVal {
    def asDomain(updatedBy: String, newUpdatedAt: LocalDateTime): Try[DomainLimitUpdate] =
      Try(DomainLimitUpdate(
        maxIntervalAmount = arg.maxAmountPerInterval,
        maxAmount = arg.maxAmountPerTxn,
        minAmount = arg.minAmountPerTxn,
        maxCount = arg.maxCountPerInterval,
        maxBalanceAmount = arg.maxBalanceAmount,
        updatedBy = updatedBy,
        updatedAt = newUpdatedAt,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
  }

}
