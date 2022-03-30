package tech.pegb.backoffice.api.transaction.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class TxnToUpdateForCancellation(reason: String, @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) {

}
