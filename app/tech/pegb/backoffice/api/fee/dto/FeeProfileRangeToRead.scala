package tech.pegb.backoffice.api.fee.dto

case class FeeProfileRangeToRead(
    id: Int,
    max: Option[BigDecimal],
    min: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal])
