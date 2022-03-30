package tech.pegb.backoffice.dao.fee.dto

import java.time.LocalDateTime

case class ThirdPartyFeeProfileToInsert(
    transactionType: Option[String],
    provider: String,
    currencyId: String,
    calculationMethod: String,
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal],
    ranges: Option[Seq[ThirdPartyFeeProfileRangeToInsert]],
    createdAt: LocalDateTime,
    createdBy: String)
