package tech.pegb.backoffice.domain.fee.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.fee
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.FeeCalculationMethod

@ImplementedBy(classOf[fee.implementation.FeeProfileModelValidations])
trait FeeProfileModelValidations {

  def assertFeeProfileAmounts(
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]): Unit

  def assertFeeProfileRequiredFields(
    calculationMethod: FeeCalculationMethod,
    maxFeeIsDefined: Boolean,
    minFeeIsDefined: Boolean,
    flatAmountIsDefined: Boolean,
    percentageAmountIsDefined: Boolean,
    rangesIsDefined: Boolean): Unit
}
