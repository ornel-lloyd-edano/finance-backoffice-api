package tech.pegb.backoffice.dao.account.entity

import java.time.LocalDateTime
import java.util.UUID

case class ExternalAccount(
    id: Int,
    uuid: UUID,
    userId: Int,
    userUuid: UUID,
    provider: String,
    accountNumber: String,
    accountHolder: String,
    currencyId: Int,
    currencyName: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}

object ExternalAccount {
  val cId = "id"
  val cUuid = "uuid"
  val cUserId = "user_id"
  val cUserUUid = "user_uuid"
  val cProvider = "provider"
  val cAccountNum = "account_number"
  val cAccountHolder = "account_holder"
  val cCurrencyId = "currency_id"
  val cCurrencyName = "currency_name"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"

  val uniqueConstraint = Seq(cId, cUuid)
  val referentialConstraint = Seq(cUserId, cCurrencyId)
}
