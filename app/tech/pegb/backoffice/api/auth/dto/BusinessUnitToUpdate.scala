package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class BusinessUnitToUpdate(@JsonProperty(required = true) name: String, @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
