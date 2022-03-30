package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime

case class TxnReversal(
    txnId: String,
    isFeeReversed: Boolean,
    reason: Option[String],
    reversedBy: String,
    reversedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) {

}
