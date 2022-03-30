package tech.pegb.backoffice.api.limit.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class LimitProfileToUpdate(
    maxAmountPerInterval: Option[BigDecimal],
    maxAmountPerTxn: Option[BigDecimal],
    minAmountPerTxn: Option[BigDecimal],
    maxCountPerInterval: Option[Int],
    maxBalanceAmount: Option[BigDecimal],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
