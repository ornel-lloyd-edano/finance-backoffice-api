package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "AggregationToRead")
case class FloatAccountAggregationToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_id", required = true) userId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_name", required = true) userName: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "account_number", required = true) accountNumber: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "type", required = true) `type`: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "main_type", required = true) mainType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency", required = true) currency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "internal_balance", required = true) internalBalance: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "external_balance", required = true) externalBalance: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "inflow", required = true) inflow: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "outflow", required = true) outflow: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "net", required = true) net: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])
