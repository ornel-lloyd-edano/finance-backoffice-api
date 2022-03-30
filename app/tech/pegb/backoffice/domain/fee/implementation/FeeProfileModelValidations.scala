package tech.pegb.backoffice.domain.fee.implementation

import tech.pegb.backoffice.domain.fee.abstraction
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.FeeCalculationMethod

trait FeeProfileModelValidations extends abstraction.FeeProfileModelValidations {

  private val Zero = BigDecimal(0)

  def assertFeeProfileAmounts(
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]): Unit = {
    maxFee.foreach { max ⇒
      assert(max >= Zero, "maximum fee can not be a negative value")
      assert(max.scale <= 2, "maximum fee amount can not have more than 2 decimal digits")
      minFee.foreach(min ⇒ assert(max >= min, "maximum amount can not be less than minimum amount"))
    }
    minFee.foreach { min ⇒
      assert(min >= Zero, "minumum fee can not be a negative value")
      assert(min.scale <= 2, "minumum fee can not have more than 2 decimal digits")
    }
    flatAmount.foreach { fAmount ⇒
      assert(fAmount >= Zero, "flat amount can not be a negative value")
      assert(fAmount.scale <= 2, "flat amount can not have more than 2 decimal digits")
    }
    percentageAmount.foreach { percentage ⇒
      assert(percentage.scale <= 4, "percentage amount can not have more than 4 decimal digits")
    }
  }

  def assertFeeProfileRequiredFields(
    calculationMethod: FeeCalculationMethod,
    maxFeeIsDefined: Boolean,
    minFeeIsDefined: Boolean,
    flatAmountIsDefined: Boolean,
    percentageAmountIsDefined: Boolean,
    rangesIsDefined: Boolean): Unit = {

    if (calculationMethod.isStairCaseType) {
      assert(!percentageAmountIsDefined, "Global Percentage Amount should not be defined if calculation method is staircase type")
      assert(!flatAmountIsDefined, "Global Flat Amount should not be defined if calculation method is staircase type")
    } else if (calculationMethod.isPercentageType) {
      assert(percentageAmountIsDefined, "Percentage Amount must be defined if calculation method is percentage type")
      assert(!flatAmountIsDefined, "Flat Amount should not be defined if calculation method is percentage type")
    } else {
      assert(flatAmountIsDefined, "Flat Amount must be defined if calculation method is flat fee type")
      assert(!percentageAmountIsDefined, "Percentage Amount should not be defined if calculation method is flat fee type")
      assert(!minFeeIsDefined, "Min fee should not be defined if calculation method is flat fee type")
      assert(!maxFeeIsDefined, "Max fee should not be defined if calculation method is flat fee type")
    }

    if (rangesIsDefined) {
      assert(calculationMethod.isStairCaseType, "Range can only be defined if calculation method is staircase type")
    }

  }
}
