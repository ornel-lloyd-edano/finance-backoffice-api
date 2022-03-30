package tech.pegb.backoffice.api.customer.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import tech.pegb.backoffice.api.customer.dto.CustomerAttributes.Address

object CardApplicationAttributes {

  @ApiModel(value = "CardApplicationToCreate")
  case class NewCardApplicationDetails(
      @JsonProperty(required = true)@ApiModelProperty(name = "card_name", required = true) cardName: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "card_pin", required = true) cardPin: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "delivery_address", required = true) deliveryAddress: Address)

  @ApiModel(value = "CardApplication")
  case class CardApplication(
      @JsonProperty(required = true)@ApiModelProperty(name = "user_id", required = true) userId: UUID,
      @JsonProperty(required = true)@ApiModelProperty(name = "operation_type", required = true) operationType: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "card_type", required = true) cardType: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "name_on_card", required = true) nameOnCard: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "card_pin", required = true) cardPin: String,
      @JsonProperty(required = true)@ApiModelProperty(name = "delivery_address", required = true) deliveryAddress: Address,
      @JsonProperty(required = true)@ApiModelProperty(name = "status", required = true) status: String)

}
