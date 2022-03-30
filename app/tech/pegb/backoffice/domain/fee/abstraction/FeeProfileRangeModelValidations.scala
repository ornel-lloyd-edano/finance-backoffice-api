package tech.pegb.backoffice.domain.fee.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.fee

@ImplementedBy(classOf[fee.implementation.FeeProfileRangeModelValidations])
trait FeeProfileRangeModelValidations {
  def assertFeeProfileRangeAmounts(
    from: BigDecimal,
    to: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]): Unit
}
