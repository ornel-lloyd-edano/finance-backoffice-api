package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "AccountToRead")
case class AccountToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_id", required = true) customerId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_name", required = true) customerName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_full_name", required = true) customerFullName: String, //TODO remove as customer name is already convering it
    @JsonProperty(required = true)@ApiModelProperty(name = "msisdn", required = true) msisdn: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "number", required = true) number: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "name", required = true) name: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "type", required = true) `type`: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "is_main_account", required = true) isMainAccount: Boolean,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency", required = true) currency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "balance", required = true) balance: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "blocked_balance", required = true) blockedBalance: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "available_balance", required = true) availableBalance: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "last_transaction_at", required = true) lastTransactionAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "main_type", required = true) mainType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])
