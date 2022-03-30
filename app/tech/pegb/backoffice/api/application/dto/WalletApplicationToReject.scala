package tech.pegb.backoffice.api.application.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class WalletApplicationToReject(
    @JsonProperty(required = true) reason: String,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
