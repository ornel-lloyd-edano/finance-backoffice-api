package tech.pegb.backoffice.api.customer.dto

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(value = "IndividualUser")
case class IndividualUserResponse(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "username", required = true) username: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "tier", required = true) tier: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "segment", required = true) segment: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "subscription", required = true) subscription: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "email", required = true) email: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: Option[String],

    @JsonProperty(required = true)@ApiModelProperty(name = "msisdn", required = true) msisdn: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "individual_user_type", required = true) individualUserType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "alias", required = true) alias: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "full_name", required = true) fullName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "gender", required = true) gender: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "person_id", required = true) personId: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_number", required = true) documentNumber: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_type", required = true) documentType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "document_model", required = true) documentModel: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "birth_date", required = true) birthDate: Option[LocalDate],
    @JsonProperty(required = true)@ApiModelProperty(name = "birth_place", required = true) birthPlace: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "nationality", required = true) nationality: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "occupation", required = true) occupation: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "company_name", required = true) companyName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "employer", required = true) employer: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "activated_at", required = true) activatedAt: Option[ZonedDateTime])
