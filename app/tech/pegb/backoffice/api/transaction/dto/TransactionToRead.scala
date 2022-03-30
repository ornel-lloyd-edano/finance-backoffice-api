package tech.pegb.backoffice.api.transaction.dto

import java.time.ZonedDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class TransactionToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", example = "1549446333", required = true) id: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "sequence", example = "1", required = true) sequence: Long,
    @JsonProperty(required = true)@ApiModelProperty(name = "primary_account_id", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true) primaryAccountId: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "primary_account_name", example = "+971507472520_standard_wallet", required = true) primaryAccountName: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "primary_account_number", example = "846.1", required = true) primaryAccountNumber: String,
    @JsonProperty(required = false)@ApiModelProperty(name = "primary_account_customer_name", example = "George Ogalo", required = false) primaryAccountCustomerName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "secondary_account_id", example = "c12ba2fa-129c-4c25-b920-2f85a6428b5f", required = false) secondaryAccountId: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "secondary_account_name", example = "+971544451680_standard_wallet", required = true) secondaryAccountName: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "secondary_account_number", example = "176.3", required = true) secondaryAccountNumber: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "direction", example = "credit", required = true) direction: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "type", example = "p2p_domestic", required = true) `type`: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "amount", example = "0", required = true) amount: BigDecimal,
    @JsonProperty(required = true)@ApiModelProperty(name = "currency", example = "AED", required = true) currency: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "exchanged_currency", example = "KES", required = false) exchangeCurrency: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "channel", example = "IOS_APP", required = true) channel: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "explanation", example = "some explanation", required = false) explanation: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "effective_rate", required = false) effectiveRate: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "cost_rate", required = false) costRate: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "status", example = "success", required = true) status: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "previous_balance", required = false) previousBalance: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "reason", required = false) reason: Option[String])
