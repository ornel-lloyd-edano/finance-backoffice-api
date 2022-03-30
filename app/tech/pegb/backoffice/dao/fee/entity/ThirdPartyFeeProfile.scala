package tech.pegb.backoffice.dao.fee.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class ThirdPartyFeeProfile(
    id: String,
    transactionType: Option[String],
    provider: String,
    currencyCode: String,
    calculationMethod: String,
    isActive: Boolean,
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal],
    ranges: Option[Seq[ThirdPartyFeeProfileRange]],
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object ThirdPartyFeeProfile {
  implicit val f = Json.format[ThirdPartyFeeProfile]
}
