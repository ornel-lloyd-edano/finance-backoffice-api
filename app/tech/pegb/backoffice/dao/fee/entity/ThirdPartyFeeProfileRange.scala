package tech.pegb.backoffice.dao.fee.entity

import play.api.libs.json.Json

case class ThirdPartyFeeProfileRange(
    id: String,
    thirdPartyFeeProfileId: String,
    max: Option[BigDecimal] = None,
    min: Option[BigDecimal] = None,
    feeAmount: Option[BigDecimal] = None,
    feeRatio: Option[BigDecimal] = None)

object ThirdPartyFeeProfileRange {
  implicit val f = Json.format[ThirdPartyFeeProfileRange]
}
