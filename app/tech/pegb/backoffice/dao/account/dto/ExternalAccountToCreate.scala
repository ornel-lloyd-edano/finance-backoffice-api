package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime
import java.util.UUID

case class ExternalAccountToCreate(
    id: Option[Int] = None,
    uuid: UUID,
    userId: Int,
    provider: String,
    accountNumber: String,
    accountHolder: String,
    currencyId: Int,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None) {

}
