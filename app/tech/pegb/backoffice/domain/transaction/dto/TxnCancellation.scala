package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime

case class TxnCancellation(
    txnId: String,
    reason: String,
    cancelledBy: String,
    cancelledAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) {

}
