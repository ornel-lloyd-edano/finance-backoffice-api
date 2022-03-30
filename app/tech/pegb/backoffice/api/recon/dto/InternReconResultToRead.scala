package tech.pegb.backoffice.api.recon.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class InternReconResultToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "incident_id", required = true) incidentId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "recon_id", required = true) reconId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "recon_date", required = true) reconDate: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "account_number", required = true) accountNumber: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency", required = true) currency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "txn_id", required = true) txnId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "txn_sequence", required = true) txnSequence: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "txn_direction", required = true) txnDirection: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "txn_date", required = true) txnDate: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "txn_amount", required = true) txnAmount: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "balance_before", required = true) balanceBefore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "balance_after", required = true) balanceAfter: Option[BigDecimal])
