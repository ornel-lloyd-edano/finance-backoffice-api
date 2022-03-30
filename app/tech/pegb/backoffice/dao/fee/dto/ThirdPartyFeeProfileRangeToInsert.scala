package tech.pegb.backoffice.dao.fee.dto

case class ThirdPartyFeeProfileRangeToInsert(
    max: Option[BigDecimal],
    min: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal])
