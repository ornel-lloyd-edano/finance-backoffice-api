package tech.pegb.backoffice.api.fee.dto

import java.time.{ZonedDateTime}
import java.util.UUID

case class FeeProfileToReadDetails(
    id: UUID,
    feeType: String,
    userType: String,
    tier: String,
    subscriptionType: String,
    transactionType: String,
    channel: Option[String],
    otherParty: Option[String],
    instrument: Option[String],
    calculationMethod: String,
    currencyCode: String,
    feeMethod: String,
    taxIncluded: Option[Boolean],
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal],
    ranges: Option[Seq[FeeProfileRangeToRead]],
    updatedAt: Option[ZonedDateTime]) {

}
