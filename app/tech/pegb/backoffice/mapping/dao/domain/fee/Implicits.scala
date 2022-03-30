package tech.pegb.backoffice.mapping.dao.domain.fee

import java.util.Currency

import tech.pegb.backoffice.dao.fee.entity.{FeeProfile, FeeProfileRange}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{UserTiers, UserType}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.TaxInclusionTypes.TaxInclusionStringAdapter
import tech.pegb.backoffice.domain.fee.model.FeeAttributes._
import tech.pegb.backoffice.domain.fee.model.{FeeProfile ⇒ DomainFeeProfile, FeeProfileRange ⇒ DomainFeeProfileRange}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}

object Implicits {

  implicit class FeeProfileRangeAdapter(val arg: FeeProfileRange) extends {
    def asDomain = DomainFeeProfileRange(
      id = arg.id,
      from = arg.min.getOrElse(BigDecimal(0)),
      to = arg.max,
      flatAmount = arg.feeAmount,
      percentageAmount = arg.feeRatio)
  }

  implicit class FeeProfileAdapter(val arg: FeeProfile) extends AnyVal {
    def asDomain = {
      DomainFeeProfile(
        id = arg.uuid,
        feeType = FeeType(arg.feeType),
        userType = UserType(arg.userType),
        tier = UserTiers.fromString(arg.tier),
        subscription = CustomerSubscription(arg.subscription),
        transactionType = TransactionType(arg.transactionType),
        channel = arg.channel.map(Channel(_)),
        otherParty = arg.provider,
        instrument = arg.instrument,
        calculationMethod = FeeCalculationMethod(arg.calculationMethod),
        currencyCode = Currency.getInstance(arg.currencyCode),
        feeMethod = FeeMethod(arg.feeMethod),
        taxInclusion = arg.taxIncluded.toTaxInclusionType,
        maxFee = arg.maxFee,
        minFee = arg.minFee,
        flatAmount = arg.feeAmount,
        percentageAmount = arg.feeRatio,
        ranges = arg.ranges.map(_.map(_.asDomain)),
        createdAt = arg.createdAt,
        updatedAt = arg.updatedAt,
        createdBy = arg.createdBy,
        updatedBy = arg.updatedBy)
    }
  }

}
