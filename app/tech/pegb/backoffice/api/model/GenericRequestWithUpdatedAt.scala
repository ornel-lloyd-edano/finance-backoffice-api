package tech.pegb.backoffice.api.model

import java.time.{ZonedDateTime}

import com.fasterxml.jackson.annotation.JsonProperty

case class GenericRequestWithUpdatedAt(
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])

object GenericRequestWithUpdatedAt {
  val empty = new GenericRequestWithUpdatedAt(lastUpdatedAt = None)
}
