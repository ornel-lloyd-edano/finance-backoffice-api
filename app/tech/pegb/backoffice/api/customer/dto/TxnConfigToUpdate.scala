package tech.pegb.backoffice.api.customer.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

trait TxnConfigToUpdateT {
  val transactionType: Option[String]
  val currency: Option[String]
  val lastUpdatedAt: Option[ZonedDateTime]
}

case class TxnConfigToUpdate(
    @JsonProperty(required = false) transactionType: Option[String],
    @JsonProperty(required = false) currency: Option[String],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) {

}
