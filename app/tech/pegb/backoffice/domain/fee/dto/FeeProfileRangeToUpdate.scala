package tech.pegb.backoffice.domain.fee.dto

case class FeeProfileRangeToUpdate(
    from: BigDecimal,
    to: Option[BigDecimal],
    flatAmount: Option[BigDecimal] = None,
    percentageAmount: Option[BigDecimal] = None) extends HasRange[BigDecimal]
