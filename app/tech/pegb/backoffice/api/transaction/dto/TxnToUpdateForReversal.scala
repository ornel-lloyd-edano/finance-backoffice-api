package tech.pegb.backoffice.api.transaction.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class TxnToUpdateForReversal(
    reason: String,
    isFeeReversed: Option[Boolean],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime]) {

}
