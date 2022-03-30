package tech.pegb.backoffice.mapping.domain.api.commission

import tech.pegb.backoffice.api.commission.dto.{CommissionProfileRangeToRead, CommissionProfileToRead, CommissionProfileToReadDetails}
import tech.pegb.backoffice.domain.commission.model.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class CommissionProfileLimitToReadAdapter(val arg: CommissionProfile) extends AnyVal {

    def asApi = {
      CommissionProfileToRead(
        id = arg.uuid,
        businessType = arg.businessType.toString,
        tier = arg.tier.toString,
        subscriptionType = arg.subscriptionType,
        transactionType = arg.transactionType,
        currencyCode = arg.currencyCode.getCurrencyCode,
        channel = arg.channel,
        instrument = arg.instrument,
        calculationMethod = arg.calculationMethod.toString,
        maxCommission = arg.maxCommission,
        minCommission = arg.minCommission,
        commissionAmount = arg.commissionAmount,
        commissionRatio = arg.commissionRatio,
        createdBy = arg.createdBy,
        updatedBy = arg.updatedBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedAt = arg.updatedAt.toZonedDateTimeUTC)
    }

    def asApiDetails = { //TODO: we can use implicit object pattern here so we can be consistent to calling only asApi
      CommissionProfileToReadDetails(
        id = arg.uuid,
        businessType = arg.businessType.toString,
        tier = arg.tier.toString,
        subscriptionType = arg.subscriptionType,
        transactionType = arg.transactionType,
        currencyCode = arg.currencyCode.getCurrencyCode,
        channel = arg.channel,
        instrument = arg.instrument,
        calculationMethod = arg.calculationMethod.toString,
        maxCommission = arg.maxCommission,
        minCommission = arg.minCommission,
        commissionAmount = arg.commissionAmount,
        commissionRatio = arg.commissionRatio,
        ranges = arg.ranges.map(_.map(_.asApi)),
        createdBy = arg.createdBy,
        updatedBy = arg.updatedBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedAt = arg.updatedAt.toZonedDateTimeUTC)
    }
  }

  implicit class CommissionProfileRangesToReadAdapter(val arg: CommissionProfileRange) extends AnyVal {
    def asApi = CommissionProfileRangeToRead(
      min = arg.min,
      max = arg.max,
      commissionAmount = arg.flatAmount,
      commissionRatio = arg.percentageAmount)
  }

}
