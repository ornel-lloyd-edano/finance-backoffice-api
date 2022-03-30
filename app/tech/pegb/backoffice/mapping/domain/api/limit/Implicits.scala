package tech.pegb.backoffice.mapping.domain.api.limit

import tech.pegb.backoffice.api.limit.dto.{LimitProfileToRead, LimitProfileToReadDetail}
import tech.pegb.backoffice.domain.limit.model.LimitProfile
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class LimitProfileAdapter(val arg: LimitProfile) extends AnyVal {
    def asApiDetailed: LimitProfileToReadDetail =
      LimitProfileToReadDetail(
        id = arg.id,
        limitType = arg.limitType.underlying,
        userType = arg.userType.underlying,
        tier = arg.tier.underlying,
        subscription = arg.subscription.underlying,
        transactionType = arg.transactionType.map(_.underlying),
        channel = arg.channel.map(_.underlying),
        otherParty = arg.otherParty,
        instrument = arg.instrument,
        interval = arg.interval.map(_.toString),
        maxBalanceAmount = arg.maxBalanceAmount,
        maxAmountPerInterval = arg.maxIntervalAmount,
        maxAmountPerTxn = arg.maxAmountPerTransaction,
        minAmountPerTxn = arg.minAmountPerTransaction,
        maxCountPerInterval = arg.maxCount,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        currencyCode = Some(arg.currencyCode.getCurrencyCode)) // TODO refactor once have the currency changes in dao

    def asApi: LimitProfileToRead =
      LimitProfileToRead(
        id = arg.id,
        limitType = arg.limitType.underlying,
        userType = arg.userType.underlying,
        tier = arg.tier.underlying,
        subscription = arg.subscription.underlying,
        transactionType = arg.transactionType.map(_.underlying),
        channel = arg.channel.map(_.underlying),
        otherParty = arg.otherParty,
        instrument = arg.instrument,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        currencyCode = Some(arg.currencyCode.getCurrencyCode)) // TODO refactor once have the currency changes in dao
  }
}
