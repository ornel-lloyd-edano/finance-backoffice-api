package tech.pegb.backoffice.domain.fee.dto

import tech.pegb.backoffice.domain.fee.model.FeeProfileRange

case class FeeProfileRangeToCreate(
    from: BigDecimal,
    to: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]) extends HasRange[BigDecimal] {
  FeeProfileRange.assertFeeProfileRangeAmounts(from = this.from, toOption = this.to,
    flatAmount = this.flatAmount, percentageAmount = this.percentageAmount)
}

object FeeProfileRangeToCreate {
  val empty = new FeeProfileRangeToCreate(from = BigDecimal(0), to = Option(BigDecimal(0)),
    flatAmount = None, percentageAmount = None)
}
