package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "LimitProfileToCreate")
case class LimitProfileToCreate(
    @ApiModelProperty(name = "limit_type", required = true) limitType: String,
    @ApiModelProperty(name = "user_type", required = true) userType: String,
    @ApiModelProperty(name = "tier", required = true) tier: String,
    @ApiModelProperty(name = "subscription", required = true) subscription: String,
    @ApiModelProperty(name = "transaction_type", required = true) transactionType: Option[String],
    @ApiModelProperty(name = "channel", required = true) channel: Option[String],
    @ApiModelProperty(name = "other_party", required = true) otherParty: Option[String],
    @ApiModelProperty(name = "instrument", required = true) instrument: Option[String],
    @ApiModelProperty(name = "interval", required = true) interval: Option[String],
    @ApiModelProperty(name = "currency_code", required = true) currencyCode: String,
    @ApiModelProperty(name = "max_amount_per_interval", required = true) maxAmountPerInterval: Option[BigDecimal],
    @ApiModelProperty(name = "min_amount_per_txn", required = true) minAmountPerTxn: Option[BigDecimal],
    @ApiModelProperty(name = "max_amount_per_txn", required = true) maxAmountPerTxn: Option[BigDecimal],
    @ApiModelProperty(name = "max_count_per_interval", required = true) maxCountPerInterval: Option[Int],
    @ApiModelProperty(name = "max_balance_amount", required = true) maxBalanceAmount: Option[BigDecimal])
