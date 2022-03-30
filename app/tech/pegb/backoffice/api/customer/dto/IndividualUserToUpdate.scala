package tech.pegb.backoffice.api.customer.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModelProperty}

case class IndividualUserToUpdate(
    @JsonProperty(required = true)@ApiModelProperty(name = "msisdn", required = true) msisdn: String)
