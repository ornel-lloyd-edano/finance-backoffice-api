package tech.pegb.backoffice.domain.fee.dto

import java.time.LocalDateTime
import java.util.Currency

import cats.implicits._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.CustomerSubscription
import tech.pegb.backoffice.domain.customer.model.{IndividualUserTiers, UserTier, UserType}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes._
import tech.pegb.backoffice.domain.fee.model.FeeProfile
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}

import scala.util.Try

case class FeeProfileToCreate(
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
    ranges: Option[Seq[FeeProfileRangeToCreate]],
    createdAt: LocalDateTime,
    createdBy: String) extends Validatable[Unit] {

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

object FeeProfileToCreate {
  val empty = new FeeProfileToCreate(
    feeType = FeeType("transaction_based"),
    userType = UserType("individual"),
    tier = IndividualUserTiers.Basic,
    subscription = CustomerSubscription("standard"),
    transactionType = TransactionType("p2p_domestic"),
    channel = Some(Channel("mobile_money")),
    otherParty = None,
    instrument = None,
    calculationMethod = FeeCalculationMethod("flat_fee"),
    currencyCode = Currency.getInstance("USD"),
    feeMethod = FeeMethod("add"),
    taxInclusion = TaxInclusionTypes.NoTax,
    maxFee = None,
    minFee = None,
    flatAmount = Some(BigDecimal(0)), //Set to non for percentage in copy
    percentageAmount = None,
    ranges = None,
    createdAt = LocalDateTime.now,
    createdBy = "some user")
}
