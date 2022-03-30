package tech.pegb.backoffice.dao.fee.dto

case class FeeProfileRangeToInsert(
    feeProfileId: Option[Int],
    max: Option[BigDecimal],
    min: BigDecimal,
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal]) {

}
