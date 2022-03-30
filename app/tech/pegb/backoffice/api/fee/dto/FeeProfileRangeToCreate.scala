package tech.pegb.backoffice.api.fee.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class FeeProfileRangeToCreate(
    @JsonProperty(required = true) max: Option[BigDecimal],
    @JsonProperty(required = true) min: BigDecimal,
    @JsonProperty(required = false) feeAmount: Option[BigDecimal],
    @JsonProperty(required = false) feeRatio: Option[BigDecimal])
