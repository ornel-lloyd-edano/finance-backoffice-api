package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class RoleToUpdate(
    name: Option[String],
    level: Option[Int],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) {
}
