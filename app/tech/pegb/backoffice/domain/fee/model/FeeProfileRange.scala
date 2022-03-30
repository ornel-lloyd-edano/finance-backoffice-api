package tech.pegb.backoffice.domain.fee.model

import tech.pegb.backoffice.domain.fee.dto.HasRange
import tech.pegb.backoffice.domain.fee.implementation.FeeProfileRangeModelValidations
import tech.pegb.backoffice.util.WithSmartString

case class FeeProfileRange(
    id: Int,
    from: BigDecimal,
    to: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]) extends HasRange[BigDecimal] with WithSmartString {

  FeeProfileRange.assertFeeProfileRangeAmounts(from = this.from, toOption = this.to,
    flatAmount = this.flatAmount, percentageAmount = this.percentageAmount)
}

object FeeProfileRange extends FeeProfileRangeModelValidations {

  val empty = new FeeProfileRange(id = -1, from = BigDecimal(0), to = None,
    flatAmount = None, percentageAmount = None)
}
