package tech.pegb.backoffice.dao.transaction.entity

import java.time.LocalDateTime
import java.util.UUID

case class TxnConfig(
    id: Int,
    uuid: UUID,
    userId: Int,
    userUuid: UUID,
    transactionType: String,
    currencyId: Int,
    currencyName: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}

object TxnConfig {
  val cId = "id"
  val cUuid = "uuid"
  val cUserId = "user_id"
  val cTxnType = "transaction_type"
  val cCurrencyId = "currency_id"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"

  val uniqueConstraint = Set(cId, cUuid)
  val referentialConstraint = Set(cUserId, cCurrencyId)
}
