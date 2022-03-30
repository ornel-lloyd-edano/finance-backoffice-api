package tech.pegb.backoffice.api.currencyexchange.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class SpreadToCreate(
    @JsonProperty(required = true) transactionType: String,
    @JsonProperty(required = true) channel: Option[String],
    @JsonProperty(required = true) institution: Option[String],
    @JsonProperty(required = true) spread: BigDecimal)
