package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class ActionExplanation(
    @JsonProperty("explanation") explanation: String,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
