package tech.pegb.backoffice.domain.fee.dto

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.{FeeCalculationMethod, FeeMethod, TaxInclusionType}
import tech.pegb.backoffice.domain.fee.model.FeeProfile
import tech.pegb.backoffice.util.LastUpdatedAt

import scala.util.Try

case class FeeProfileToUpdate(
    calculationMethod: FeeCalculationMethod,
    feeMethod: FeeMethod,
    taxInclusion: TaxInclusionType,
    maxFee: Option[BigDecimal] = None,
    minFee: Option[BigDecimal] = None,
    flatAmount: Option[BigDecimal] = None,
    percentageAmount: Option[BigDecimal] = None,
    ranges: Option[Seq[FeeProfileRangeToCreate]],
    deletedAt: Option[LocalDateTime] = None,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]) extends Validatable[Unit] with LastUpdatedAt {

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
    } yield {
      ()
    }
  }
}
