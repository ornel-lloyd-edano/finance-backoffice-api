package tech.pegb.backoffice.dao.fee.entity

import play.api.libs.json.Json

case class FeeProfileRange(
    id: Int,
    feeProfileId: Option[Int],
    max: Option[BigDecimal],
    min: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal])

object FeeProfileRange {
  implicit val f = Json.format[FeeProfileRange]
}
