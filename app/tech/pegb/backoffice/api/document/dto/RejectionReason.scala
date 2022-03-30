package tech.pegb.backoffice.api.document.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class RejectionReason(
    @JsonProperty(required = true) reason: String,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])

