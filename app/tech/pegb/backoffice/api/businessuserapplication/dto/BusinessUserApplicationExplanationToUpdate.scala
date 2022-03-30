package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class BusinessUserApplicationExplanationToUpdate(
    explanation: String,
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) extends BusinessUserApplicationExplanationToUpdateT

trait BusinessUserApplicationExplanationToUpdateT {
  def explanation: String
  def lastUpdatedAt: Option[ZonedDateTime]
}
