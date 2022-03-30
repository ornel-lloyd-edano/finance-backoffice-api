package tech.pegb.backoffice.api.recon.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class InternReconSummaryToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "account_number", required = true) accountNumber: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "account_type", required = true) accountType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "account_main_type", required = true) accountMainType: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency", required = true) currency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_id", required = true) userId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "user_full_name", required = true) userFullName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_name", required = true) customerName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "date", required = true) date: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "total_value", required = true) totalValue: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "difference", required = true) difference: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "total_txn", required = true) totalTxn: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "txn_count", required = true) txnCount: Int,
    @JsonProperty(required = true)@ApiModelProperty(name = "incidents", required = true) incidents: Int,
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "comments", required = true) comments: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])

