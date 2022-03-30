package tech.pegb.backoffice.api.fee.dto

case class FeeProfileRangeToUpdate(
    max: Option[BigDecimal],
    min: BigDecimal,
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal])
