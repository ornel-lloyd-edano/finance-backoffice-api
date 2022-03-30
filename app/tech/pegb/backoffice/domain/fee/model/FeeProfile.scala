package tech.pegb.backoffice.domain.fee.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import cats.implicits._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{UserTier, UserType}
import tech.pegb.backoffice.domain.fee.dto.HasRange
import tech.pegb.backoffice.domain.fee.implementation.FeeProfileModelValidations
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{TaxInclusionType, _}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.util.WithSmartString

import scala.util.Try

case class FeeProfile(
    id: UUID,
    feeType: FeeType,
    userType: UserType,
    tier: UserTier,
    subscription: CustomerSubscription,
    transactionType: TransactionType,
    channel: Option[Channel],
    otherParty: Option[String],
    instrument: Option[String],
    calculationMethod: FeeCalculationMethod,
    currencyCode: Currency,
    feeMethod: FeeMethod,
    taxInclusion: TaxInclusionType,
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal],
    ranges: Option[Seq[FeeProfileRange]],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime],
    createdBy: String,
    updatedBy: Option[String]) extends Validatable[Unit] with WithSmartString {

  def validate: ServiceResponse[Unit] = {
    for {
      _ ← Try(FeeProfile.assertFeeProfileRequiredFields(
        calculationMethod = this.calculationMethod,
        maxFeeIsDefined = this.maxFee.isDefined,
        minFeeIsDefined = this.minFee.isDefined,
        flatAmountIsDefined = this.flatAmount.isDefined,
        percentageAmountIsDefined = this.percentageAmount.isDefined,
        rangesIsDefined = this.ranges.isDefined)).toEither
        .leftMap(t ⇒ ServiceError.validationError(t.getMessage))
      _ ← Try(FeeProfile.assertFeeProfileAmounts(maxFee = this.maxFee, minFee = this.minFee,
        flatAmount = this.flatAmount, percentageAmount = this.percentageAmount)).toEither
        .leftMap(t ⇒ ServiceError.validationError(t.getMessage))
      _ ← FeeProfile.validateRangeAmount(this.ranges, this.calculationMethod)
      _ ← FeeProfile.validateRange(this.ranges)
      _ ← FeeProfile.validateTier(this.tier, this.userType)
    } yield {
      ()
    }
  }
}

object FeeProfile extends FeeProfileModelValidations {

  def validateTier(tier: UserTier, userType: UserType): ServiceResponse[Unit] = {
    for {
      _ ← tier.validate
      _ ← userType.underlying match {
        case "individual" ⇒ Either.cond(tier.isIndividualUserTier, (), ServiceError.validationError(s"Tier $tier is not a valid IndividualUser Tier"))
        case "business" ⇒ Either.cond(tier.isBusinessUserTier, (), ServiceError.validationError(s"Tier $tier is not a valid BusinessUser Tier"))
        case _ ⇒ ().asRight[ServiceError]
      }
    } yield {
      ()
    }
  }

  def validateRangeAmount(rangeOption: Option[Seq[HasRange[BigDecimal]]], calculationMethod: FeeCalculationMethod): ServiceResponse[String] = {
    rangeOption match {
      case None ⇒ Right("valid")
      case Some(ranges) ⇒
        if (calculationMethod.isPercentageType &&
          !ranges.forall(_.percentageAmount.isDefined) &&
          !ranges.forall(_.flatAmount.isEmpty)) {
          Left(ServiceError.validationError("All percentageAmount should be defined AND all flatAmount should be empty for percentage staircase type calculationMethod"))
        } else if (!calculationMethod.isPercentageType &&
          !ranges.forall(_.flatAmount.isDefined) &&
          !ranges.forall(_.percentageAmount.isEmpty)) {
          Left(ServiceError.validationError("All flatAmount should be defined AND all percentageAmount should be empty for Non-percentage staircase type calculationMethod"))
        } else {
          Right("valid")
        }
    }
  }

  def validateRange(rangeOption: Option[Seq[HasRange[BigDecimal]]]): ServiceResponse[String] = {
    def helper(remaining: Seq[HasRange[BigDecimal]], prevTo: BigDecimal): ServiceResponse[String] = {
      remaining match {
        case Nil ⇒ Right("valid")
        case head :: tail ⇒
          head.to match {
            case Some(toValue) ⇒
              if (head.from > toValue) {
                Left(ServiceError.validationError("Range 'from' value cannot be larger than 'to' value"))
              } else if (head.from != prevTo) {
                Left(ServiceError.validationError(s"Current range 'from' value (${head.from}) should be equal to Previous Range 'to' value (${prevTo})"))
              } else {
                helper(tail, toValue)
              }

            case None ⇒
              if (tail.nonEmpty) {
                Left(ServiceError.validationError("Range 'to' value can only be empty for the last range element"))
              } else if (head.from != prevTo) {
                Left(ServiceError.validationError(s"Current range 'from' value (${head.from}) should be equal to Previous Range 'to' value (${prevTo})"))
              } else {
                Right("valid")
              }
          }
      }
    }

    rangeOption match {
      case Some(ranges) if !ranges.isEmpty ⇒ helper(ranges, ranges.head.from)
      case None ⇒ Right("valid")
    }
  }
}
