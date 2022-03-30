package tech.pegb.backoffice.api.customer.dto

import java.time.LocalDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.CustomerAttributes.Address

@ApiModel(value = "ActivatedBusinessUserToRead")
case class ActivatedBusinessUserToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "username", required = true) username: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "password", required = true) password: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "tier", required = true) tier: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "segment", required = true) segment: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "subscription", required = true) subscription: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "emails", required = true) emails: Set[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String,

    @JsonProperty(required = true)@ApiModelProperty(name = "name", required = true) name: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "addresses", required = true) addresses: Set[Address],
    @JsonProperty(required = true)@ApiModelProperty(name = "phone_numbers", required = true) phoneNumbers: Set[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "activation_requirements", required = true) activationRequirements: Set[String],

    @JsonProperty(required = true)@ApiModelProperty(name = "accounts", required = true) accounts: Set[AccountToRead],
    @JsonProperty(required = true)@ApiModelProperty(name = "activated_at", required = true) activatedAt: Option[LocalDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "password_updated_at", required = true) passwordUpdatedAt: Option[LocalDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: LocalDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[LocalDateTime],
    @JsonProperty(required = true)@ApiModelProperty(name = "updated_by", required = true) updatedBy: Option[String])

@ApiModel(value = "RegisteredButNotActivatedBusinessUserToRead")
case class RegisteredButNotActivatedBusinessUserToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "username", required = true) username: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "email", required = true) email: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "name", required = true) name: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "accounts", required = true) accounts: Set[AccountToRead],
    @JsonProperty(required = true)@ApiModelProperty(name = "created_at", required = true) createdAt: LocalDateTime,
    @JsonProperty(required = true)@ApiModelProperty(name = "created_by", required = true) createdBy: String)

@ApiModel(value = "GetBusinessUserResult")
case class GetBusinessUserResult(
    @JsonProperty(required = true)@ApiModelProperty(name = "count", required = true) count: Int,
    @JsonProperty(required = true)@ApiModelProperty(name = "result", required = true) result: Seq[ActivatedBusinessUserToRead])
