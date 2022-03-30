package tech.pegb.backoffice.domain.commission.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessType, BusinessUserTier}
import tech.pegb.backoffice.domain.commission.dto.CommissionProfileRangeToCreate
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethod

case class CommissionProfile(
    id: Int,
    uuid: UUID,
    businessType: BusinessType,
    tier: BusinessUserTier,
    subscriptionType: String,
    transactionType: String,
    currencyId: Int,
    currencyCode: Currency,
    channel: Option[String],
    instrument: Option[String],
    calculationMethod: CommissionCalculationMethod,
    maxCommission: Option[BigDecimal],
    minCommission: Option[BigDecimal],
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal],
    ranges: Option[Seq[CommissionProfileRange]],
    createdBy: String,
    updatedBy: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: Option[LocalDateTime]) {

  def addRanges(rangesToAdd: Option[Seq[CommissionProfileRange]]): CommissionProfile = {
    this.copy(ranges = rangesToAdd)
  }
}

object CommissionProfile {

  private val Zero = BigDecimal(0)

  def validateRangeAmount(rangeOption: Option[Seq[CommissionProfileRangeToCreate]], calculationMethod: CommissionCalculationMethod): ServiceResponse[String] = {
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

  def validateRange(rangeOption: Option[Seq[CommissionProfileRangeToCreate]]): ServiceResponse[String] = {
    def helper(remaining: Seq[CommissionProfileRangeToCreate], prevTo: BigDecimal): ServiceResponse[String] = {
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

  def assertCommissionProfileAmounts(
    maxCommission: Option[BigDecimal],
    minCommission: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]): Unit = { //TODO: return ServiceResponse
    maxCommission.foreach { max ⇒
      assert(max >= Zero, "maximum commission can not be a negative value")
      assert(max.scale <= 2, "maximum commission amount can not have more than 2 decimal digits")
      minCommission.foreach(min ⇒ assert(max >= min, "maximum amount can not be less than minimum amount"))
    }
    minCommission.foreach { min ⇒
      assert(min >= Zero, "minumum commission can not be a negative value")
      assert(min.scale <= 2, "minumum commission can not have more than 2 decimal digits")
    }
    flatAmount.foreach { fAmount ⇒
      assert(fAmount >= Zero, "flat amount can not be a negative value")
      assert(fAmount.scale <= 2, "flat amount can not have more than 2 decimal digits")
    }
    percentageAmount.foreach { percentage ⇒
      assert(percentage.scale <= 4, "percentage amount can not have more than 4 decimal digits")
    }
  }

  def assertCommissionProfileRequiredFields(
    calculationMethod: CommissionCalculationMethod,
    maxCommissionIsDefined: Boolean,
    minCommissionIsDefined: Boolean,
    flatAmountIsDefined: Boolean,
    percentageAmountIsDefined: Boolean,
    rangesIsDefined: Boolean): Unit = { //TODO: return ServiceResponse

    if (calculationMethod.isStaircaseType) {
      assert(!percentageAmountIsDefined, "Global Percentage Amount should not be defined if calculation method is staircase type")
      assert(!flatAmountIsDefined, "Global Flat Amount should not be defined if calculation method is staircase type")
    } else if (calculationMethod.isPercentageType) {
      assert(percentageAmountIsDefined, "Percentage Amount must be defined if calculation method is percentage type")
      assert(!flatAmountIsDefined, "Flat Amount should not be defined if calculation method is percentage type")
    } else {
      assert(flatAmountIsDefined, "Flat Amount must be defined if calculation method is flat commission type")
      assert(!percentageAmountIsDefined, "Percentage Amount should not be defined if calculation method is flat commission type")
      assert(!minCommissionIsDefined, "Min commission should not be defined if calculation method is flat commission type")
      assert(!maxCommissionIsDefined, "Max commission should not be defined if calculation method is flat commission type")
    }

    if (rangesIsDefined) {
      assert(calculationMethod.isStaircaseType, "Range can only be defined if calculation method is staircase type")
    }

  }
}
