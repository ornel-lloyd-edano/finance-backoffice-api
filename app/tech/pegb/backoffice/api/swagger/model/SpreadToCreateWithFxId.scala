package tech.pegb.backoffice.api.swagger.model

import java.util.UUID

import io.swagger.annotations.ApiModelProperty

case class SpreadToCreateWithFxId(
    @ApiModelProperty(name = "currency_exchange_id", required = true) currencyExchangeId: UUID,
    @ApiModelProperty(name = "transaction_type", required = true, allowableValues = "currency_exchange,international_remittance") transactionType: String,
    @ApiModelProperty(name = "channel", required = true) channel: Option[String],
    @ApiModelProperty(name = "institution", required = true) institution: Option[String],
    @ApiModelProperty(name = "spread", required = true) spread: BigDecimal)
