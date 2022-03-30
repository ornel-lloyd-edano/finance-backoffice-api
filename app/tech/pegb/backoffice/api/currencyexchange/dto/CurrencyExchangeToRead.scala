package tech.pegb.backoffice.api.currencyexchange.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class CurrencyExchangeToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "sell_currency", required = true) sellCurrency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "buy_currency", required = true) buyCurrency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency_description", required = true) currencyDescription: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "rate", required = true) rate: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "provider", required = true) provider: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "balance", required = true) balance: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])
