package tech.pegb.backoffice.api.currencyexchange.dto

import java.time.LocalDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class SpreadToUpdate(
    spread: BigDecimal,
    @JsonProperty("updated_at") lastUpdatedAt: Option[LocalDateTime])
