package tech.pegb.backoffice.api.customer.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class CustomerAccountToCreate(
    @JsonProperty(required = true) `type`: String,
    @JsonProperty(required = true) currency: String)
