package tech.pegb.backoffice.api.currencyexchange.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

case class SpreadToCreateWithFxId(
    @JsonProperty(required = true) currencyExchangeId: UUID,
    @JsonProperty(required = true) transactionType: String,
    @JsonProperty(required = true) channel: Option[String],
    @JsonProperty(required = true) institution: Option[String],
    @JsonProperty(required = true) spread: BigDecimal)
