package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class VelocityPortalResetPinRequest(
    @JsonProperty("reason") reason: String,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
