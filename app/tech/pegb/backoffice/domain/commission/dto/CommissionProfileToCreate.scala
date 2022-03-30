package tech.pegb.backoffice.domain.commission.dto

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import cats.implicits._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessType, BusinessUserTier}
import tech.pegb.backoffice.domain.commission.model.CommissionProfile
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethod

import scala.util.Try

case class CommissionProfileToCreate(
    uuid: UUID,
    businessType: BusinessType,
    tier: BusinessUserTier,
    subscriptionType: String,
    transactionType: TransactionType,
    currencyCode: Currency,
    channel: Option[String],
    instrument: Option[String],
    calculationMethod: CommissionCalculationMethod,
    maxCommission: Option[BigDecimal],
    minCommission: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal],
    ranges: Option[Seq[CommissionProfileRangeToCreate]],
    createdBy: String,
    createdAt: LocalDateTime) extends Validatable[Unit] {

  def validate: ServiceResponse[Unit] = {
    for {
      _ ← Try(CommissionProfile.assertCommissionProfileRequiredFields(
        calculationMethod = this.calculationMethod,
        maxCommissionIsDefined = this.maxCommission.isDefined,
        minCommissionIsDefined = this.minCommission.isDefined,
        flatAmountIsDefined = this.flatAmount.isDefined,
        percentageAmountIsDefined = this.percentageAmount.isDefined,
        rangesIsDefined = this.ranges.isDefined)).toEither
        .leftMap(t ⇒ ServiceError.validationError(t.getMessage))
      _ ← Try(CommissionProfile.assertCommissionProfileAmounts(maxCommission = this.maxCommission, minCommission = this.minCommission,
        flatAmount = this.flatAmount, percentageAmount = this.percentageAmount)).toEither
        .leftMap(t ⇒ ServiceError.validationError(t.getMessage))
      _ ← CommissionProfile.validateRangeAmount(this.ranges, this.calculationMethod)
      _ ← CommissionProfile.validateRange(this.ranges)
      _ ← tier.validate
      _ ← calculationMethod.validate
      _ ← businessType.validate
    } yield {
      ()
    }
  }
}
