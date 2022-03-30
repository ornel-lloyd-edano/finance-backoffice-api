package tech.pegb.backoffice.api.customer.dto

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class GenericUserToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "username", required = true) username: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "tier", required = true) tier: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "segment", required = true) segment: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "subscription", required = true) subscription: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "email", required = true) email: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_type", required = true) customerType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: ZonedDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "activated_at", required = true) activatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "password_updated_at", required = true) passwordUpdatedAt: Option[ZonedDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "customer_name", required = true) customerName: Option[String],

    @JsonProperty(required = true)@ApiModelProperty(name = "msisdn", required = true) msisdn: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "individual_user_type", required = true) individualUserType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "alias", required = true) alias: Option[String],
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

    @JsonProperty(required = true)@ApiModelProperty(name = "business_name", required = true, example = "Universal Catering Co") businessName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "brand_name", required = true, example = "Costa Coffee DSO") brandName: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "business_type", required = true, example = "Merchant") businessType: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "business_category", required = true, example = "Restaurants - 5812") businessCategory: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "registration_number", required = true, example = "213/564654EE") registrationNumber: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "tax_number", required = true, example = "A213546468977M") taxNumber: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "registration_date", required = true, example = "1996-01-20") registrationDate: Option[LocalDate])
