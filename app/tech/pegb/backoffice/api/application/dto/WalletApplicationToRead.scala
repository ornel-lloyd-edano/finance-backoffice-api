package tech.pegb.backoffice.api.application.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "WalletApplicationToRead")
case class WalletApplicationToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_id", required = true) customerId: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "full_name", required = true) fullName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "person_id", required = true) personId: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "msisdn", required = false) msisdn: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "application_stage", required = true) applicationStage: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "applied_at", required = true) appliedAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "checked_at", required = false) checkedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "checked_by", required = false) checkedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "reason_if_rejected", required = false) reasonIfRejected: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "total_score", required = true) totalScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime])
