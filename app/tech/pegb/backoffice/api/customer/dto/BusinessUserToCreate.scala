package tech.pegb.backoffice.api.customer.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.CardApplicationAttributes.NewCardApplicationDetails

@ApiModel(value = "BusinessUserToCreate")
case class BusinessUserToCreate(
    @JsonProperty(required = true)@ApiModelProperty(name = "username", required = true) username: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "msisdn", required = true) msisdn: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "email", required = true) email: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "company", required = true) company: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "tier", required = true) tier: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "subscription", required = true) subscription: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "new_card_application_details", required = true) newCardApplicationDetails: Option[NewCardApplicationDetails])

