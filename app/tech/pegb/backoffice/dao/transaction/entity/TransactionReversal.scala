package tech.pegb.backoffice.dao.transaction.entity

import java.time.LocalDateTime

case class TransactionReversal(
    id: Long,
    reversedTransactionId: String,
    reversalTransactionId: String,
    reason: String,
    status: String,
    createdBy: String,
    updatedBy: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime)
