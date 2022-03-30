package tech.pegb.backoffice.api.fee.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class FeeProfileToUpdate(
    calculationMethod: String,
    feeMethod: String,
    taxIncluded: Option[Boolean] = None,
    maxFee: Option[BigDecimal] = None,
    minFee: Option[BigDecimal] = None,
    feeAmount: Option[BigDecimal] = None,
    feeRatio: Option[BigDecimal] = None,
    ranges: Option[Seq[FeeProfileRangeToCreate]] = None,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) {
}
