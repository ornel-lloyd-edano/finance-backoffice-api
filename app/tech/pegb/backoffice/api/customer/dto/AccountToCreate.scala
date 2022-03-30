package tech.pegb.backoffice.api.customer.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

case class AccountToCreate(
    @JsonProperty(required = true) customerId: UUID,
    @JsonProperty(required = true) `type`: String,
    @JsonProperty(required = true) currency: String)
