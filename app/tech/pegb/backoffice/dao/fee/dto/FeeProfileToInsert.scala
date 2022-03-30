package tech.pegb.backoffice.dao.fee.dto

import java.time.LocalDateTime

case class FeeProfileToInsert(
    feeType: String,
    userType: String,
    tier: String,
    subscriptionType: String,
    transactionType: String,
    channel: Option[String],
    provider: Option[String],
    instrument: Option[String],
    calculationMethod: String,
    currencyId: Int,
    feeMethod: String,
    taxIncluded: String,
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal],
    ranges: Option[Seq[FeeProfileRangeToInsert]],
    createdAt: LocalDateTime,
    createdBy: String) {

}
