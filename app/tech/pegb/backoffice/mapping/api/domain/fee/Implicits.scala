package tech.pegb.backoffice.mapping.api.domain.fee

import java.time.LocalDateTime
import java.util.Currency

import tech.pegb.backoffice.api.fee.dto.{FeeProfileRangeToCreate, FeeProfileRangeToUpdate, FeeProfileToCreate, FeeProfileToUpdate}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{UserTiers, UserType}
import tech.pegb.backoffice.domain.fee.dto.{FeeProfileCriteria, FeeProfileRangeToCreate ⇒ DomainFeeProfileRangeToCreate, FeeProfileRangeToUpdate ⇒ DomainFeeProfileRangeToUpdate, FeeProfileToCreate ⇒ DomainFeeProfileToCreate, FeeProfileToUpdate ⇒ DomainFreeProfileToUpdate}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.TaxInclusionTypes.{TaxInclusionBooleanAdapter, TaxInclusionStringAdapter}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes._
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object Implicits {

  implicit class FeeProfileCriteriaAdapter(val arg: (Option[UUIDLike], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Set[String])) extends AnyVal {
    def asDomain: Try[FeeProfileCriteria] =
      Try(
        FeeProfileCriteria(
          id = arg._1,
          feeType = arg._2.map(FeeType),
          userType = arg._3.map(UserType),
          tier = arg._4.map(UserTiers.fromString(_)),
          subscription = arg._5.map(CustomerSubscription),
          transactionType = arg._6.map(TransactionType),
          channel = arg._7.map(Channel),
          otherParty = arg._8,
          instrument = arg._9,
          calculationMethod = arg._10.map(FeeCalculationMethod(_)),
          currencyCode = arg._11.map(Currency.getInstance(_)),
          feeMethod = arg._12.map(FeeMethod),
          taxInclusion = arg._13.map(t ⇒ t.toTaxInclusionType),
          partialMatchFields = arg._14))
  }

  implicit class FeeProfileRangeToCreateAdapter(val arg: FeeProfileRangeToCreate) extends AnyVal {
    def asDomain: Try[DomainFeeProfileRangeToCreate] = Try {
      DomainFeeProfileRangeToCreate(
        from = arg.min,
        to = arg.max,
        flatAmount = arg.feeAmount,
        percentageAmount = arg.feeRatio)
    }
  }

  implicit class FeeProfileRangeToUpdateAdapter(val arg: FeeProfileRangeToUpdate) extends AnyVal {
    def asDomain(isAmountPercentage: Boolean): Try[DomainFeeProfileRangeToUpdate] = Try {
      DomainFeeProfileRangeToUpdate(
        from = arg.min,
        to = arg.max,
        flatAmount = arg.feeAmount,
        percentageAmount = arg.feeRatio)
    }
  }

  implicit class FeeProfileToCreateAdapter(val arg: FeeProfileToCreate) extends AnyVal {
    def asDomain(doneAt: LocalDateTime, doneBy: String): Try[DomainFeeProfileToCreate] = Try {
      DomainFeeProfileToCreate(
        feeType = FeeType(arg.feeType),
        userType = UserType(arg.userType),
        tier = UserTiers.fromString(arg.tier),
        subscription = CustomerSubscription(arg.subscriptionType),
        transactionType = TransactionType(arg.transactionType),
        channel = arg.channel.map(Channel(_)),
        otherParty = arg.otherParty,
        instrument = arg.instrument,
        calculationMethod = FeeCalculationMethod(arg.calculationMethod),
        currencyCode = Currency.getInstance(arg.currencyCode),
        feeMethod = FeeMethod(arg.feeMethod),
        taxInclusion = arg.taxIncluded.toTaxInclusionType,
        maxFee = arg.maxFee,
        minFee = arg.minFee,
        flatAmount = arg.feeAmount,
        percentageAmount = arg.feeRatio,
        ranges = arg.ranges.map(_.map(_.asDomain.get)),
        createdAt = doneAt,
        createdBy = doneBy)
    }
  }

  implicit class FeeProfileToUpdateAdapter(val arg: FeeProfileToUpdate) extends AnyVal {
    def asDomain(doneAt: LocalDateTime, doneBy: String): Try[DomainFreeProfileToUpdate] = Try {
      DomainFreeProfileToUpdate(
        calculationMethod = FeeCalculationMethod(arg.calculationMethod),
        feeMethod = FeeMethod(arg.feeMethod),
        taxInclusion = arg.taxIncluded.toTaxInclusionType,
        maxFee = arg.maxFee,
        minFee = arg.minFee,
        flatAmount = arg.feeAmount,
        percentageAmount = arg.feeRatio,
        ranges = arg.ranges.map(_.map(_.asDomain.get)),
        updatedAt = doneAt,
        updatedBy = doneBy,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }

}
