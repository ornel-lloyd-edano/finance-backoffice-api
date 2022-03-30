package tech.pegb.backoffice.dao.fee.dto

case class FeeProfileRangeToUpdate(
    max: Option[BigDecimal],
    min: BigDecimal,
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal])
