package tech.pegb.backoffice.api.limit.dto

import java.time.{ZonedDateTime}
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class LimitProfileToReadDetail(
    //common
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "limit_type", required = true) limitType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_type", required = true) userType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "tier", required = true) tier: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "subscription", required = true) subscription: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "transaction_type", required = true) transactionType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "channel", required = true) channel: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "other_party", required = true) otherParty: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "instrument", required = true) instrument: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "currency_code", required = true) currencyCode: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime],

    //balance_based
    @JsonProperty(required = true)@ApiModelProperty(name = "max_balance_amount", required = true) maxBalanceAmount: Option[BigDecimal],

    //transaction_based
    @JsonProperty(required = true)@ApiModelProperty(name = "interval", required = true) interval: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "max_amount_per_interval", required = true) maxAmountPerInterval: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "max_amount_per_txn", required = true) maxAmountPerTxn: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "min_amount_per_txn", required = true) minAmountPerTxn: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "max_count_per_interval", required = true) maxCountPerInterval: Option[Int])
