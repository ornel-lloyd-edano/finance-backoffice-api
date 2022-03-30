package tech.pegb.backoffice.api.application.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "WalletApplicationDetail")
case class WalletApplicationDetail(
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
    @JsonProperty(required = true)@ApiModelProperty(name = "fullname_score", required = true) fullNameScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "fullname_original", required = false) fullNameOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "fullname_updated", required = false) fullNameUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "birthdate_score", required = false) birthdateScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "birthdate_original", required = false) birthdateOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "birthdate_updated", required = false) birthdateUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "birthplace_score", required = false) birthplaceScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "birthplace_original", required = false) birthplaceOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "birthplace_updated", required = false) birthplaceUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "gender_score", required = false) genderScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "gender_original", required = false) genderOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "gender_updated", required = false) genderUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "nationality_score", required = false) nationalityScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "nationality_original", required = false) nationalityOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "nationality_updated", required = false) nationalityUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "person_id_score", required = false) personIdScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "person_id_original", required = false) personIdOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "person_id_updated", required = false) personIdUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_number_score", required = false) documentNumberScore: Option[BigDecimal],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_number_original", required = false) documentNumberOriginal: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_number_updated", required = false) documentNumberUpdated: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_type", required = false) documentType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_model", required = false) documentModel: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = false) updatedAt: Option[ZonedDateTime])
