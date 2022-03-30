package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "SpreadToCreate")
case class SpreadToCreate(
    @ApiModelProperty(name = "transaction_type", required = true, allowableValues = "currency_exchange,international_remittance") transactionType: String,
    @ApiModelProperty(name = "channel", required = true) channel: Option[String],
    @ApiModelProperty(name = "institution", required = true) institution: Option[String],
    @ApiModelProperty(name = "spread", required = true) spread: BigDecimal)
