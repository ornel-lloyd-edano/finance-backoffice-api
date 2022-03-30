package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

trait ExternalAccountToUpdateT {
  val provider: Option[String]
  val accountNumber: Option[String]
  val accountHolder: Option[String]
  val currency: Option[String]
  val lastUpdatedAt: Option[ZonedDateTime]
}

case class ExternalAccountToUpdate(
    @JsonProperty(required = false) provider: Option[String],
    @JsonProperty(required = false) accountNumber: Option[String],
    @JsonProperty(required = false) accountHolder: Option[String],
    @JsonProperty(required = false) currency: Option[String],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) extends ExternalAccountToUpdateT {

}
