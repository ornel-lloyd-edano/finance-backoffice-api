package tech.pegb.backoffice.dao.transaction.dto

import java.time.LocalDateTime
import java.util.UUID

case class TxnConfigToCreate(
    uuid: UUID,
    userId: Int,
    transactionType: String,
    currencyId: Int,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}
