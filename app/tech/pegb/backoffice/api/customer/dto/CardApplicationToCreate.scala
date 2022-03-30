package tech.pegb.backoffice.api.customer.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.CustomerAttributes.Address

@ApiModel(value = "CardApplicationToCreate")
case class CardApplicationToCreate(
    @JsonProperty(required = true)@ApiModelProperty(name = "card_pin", required = true) cardPin: Option[String],
    @JsonProperty(required = true)@ApiModelProperty(name = "name_on_card", required = true) nameOnCard: String,
    @JsonProperty(required = true)@ApiModelProperty(name = "card_delivery_address", required = true) cardDeliveryAddress: Address)
