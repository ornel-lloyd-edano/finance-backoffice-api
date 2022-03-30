package tech.pegb.backoffice.api.auth.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class ScopeToUpdate(
    description: Option[String] = None,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) extends ScopeToUpdateT

trait ScopeToUpdateT {
  def description: Option[String]
  def lastUpdatedAt: Option[ZonedDateTime]
}
