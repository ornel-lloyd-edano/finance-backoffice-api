package tech.pegb.backoffice.api.currencyexchange.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "SpreadToRead")
case class SpreadToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency_exchange_id", required = true) currencyExchangeId: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "buy_currency", required = true) buyCurrency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "sell_currency", required = true) sellCurrency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "transaction_type", required = true) transactionType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "channel", required = false) channel: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "institution", required = false) institution: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "spread", required = true) spread: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_by", required = false) updatedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime])
