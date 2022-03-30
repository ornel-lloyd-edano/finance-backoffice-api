package tech.pegb.backoffice.mapping.domain.api.fee

import tech.pegb.backoffice.api.fee.dto.{FeeProfileRangeToRead, FeeProfileToRead, FeeProfileToReadDetails}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.TaxInclusionTypes._
import tech.pegb.backoffice.domain.fee.model.{FeeProfile, FeeProfileRange}

object Implicits {

  implicit class FeeProfileLimitToReadAdapter(val arg: FeeProfile) extends AnyVal {
    def asApi = {
      FeeProfileToRead(
        id = arg.id,
        feeType = arg.feeType.underlying,
        userType = arg.userType.underlying,
        tier = arg.tier.toString,
        subscriptionType = arg.subscription.underlying,
        transactionType = arg.transactionType.underlying,
        channel = arg.channel.map(_.underlying),
        otherParty = arg.otherParty,
        instrument = arg.instrument,
        calculationMethod = arg.calculationMethod.underlying,
        currencyCode = arg.currencyCode.getCurrencyCode,
        feeMethod = arg.feeMethod.underlying,
        taxIncluded = arg.taxInclusion.toOptBoolean,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }

    def asApiDetails = {
      FeeProfileToReadDetails(
        id = arg.id,
        feeType = arg.feeType.underlying,
        userType = arg.userType.underlying,
        tier = arg.tier.toString,
        subscriptionType = arg.subscription.underlying,
        transactionType = arg.transactionType.underlying,
        channel = arg.channel.map(_.underlying),
        otherParty = arg.otherParty,
        instrument = arg.instrument,
        calculationMethod = arg.calculationMethod.underlying,
        currencyCode = arg.currencyCode.getCurrencyCode,
        feeMethod = arg.feeMethod.underlying,
        taxIncluded = arg.taxInclusion.toOptBoolean,
        maxFee = arg.maxFee,
        minFee = arg.minFee,
        feeAmount = arg.flatAmount,
        feeRatio = arg.percentageAmount,
        ranges = arg.ranges.map(_.map(_.asApi)),
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

  implicit class FeeProfileRangeAdapter(val arg: FeeProfileRange) extends AnyVal {
    def asApi = FeeProfileRangeToRead(
      id = arg.id,
      max = arg.to,
      min = Option(arg.from),
      feeAmount = arg.flatAmount,
      feeRatio = arg.percentageAmount)
  }

}
