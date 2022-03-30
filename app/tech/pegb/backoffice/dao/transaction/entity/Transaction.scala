package tech.pegb.backoffice.dao.transaction.entity

import java.time.LocalDateTime
import java.util.UUID
import tech.pegb.backoffice.util.UUIDLike

case class Transaction(
    id: String,
    uniqueId: String,
    sequence: Long,
    primaryAccountInternalId: Int,
    primaryAccountUuid: UUID,
    primaryAccountName: String,
    primaryAccountNumber: String,
    primaryUserUuid: UUID,
    primaryUserType: Option[String] = None,
    primaryUsername: Option[String] = None,
    primaryIndividualUsersName: Option[String] = None,
    primaryIndividualUsersFullname: Option[String] = None,
    primaryBusinessUsersBusinessName: Option[String] = None,
    primaryBusinessUsersBrandName: Option[String] = None,
    primaryAccountType: String,
    secondaryAccountInternalId: Int,
    secondaryAccountUuid: UUID,
    secondaryAccountName: String,
    secondaryAccountNumber: String,
    secondaryUserUuid: UUID,
    direction: Option[String],
    `type`: Option[String],
    amount: Option[BigDecimal],
    currency: Option[String],
    exchangedCurrency: Option[String],
    channel: Option[String],
    explanation: Option[String],
    effectiveRate: Option[BigDecimal],
    costRate: Option[BigDecimal],
    status: Option[String],
    instrument: Option[String],
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime],
    primaryAccountPreviousBalance: Option[BigDecimal],
    secondaryAccountPreviousBalance: Option[BigDecimal],
    provider: Option[String])

object Transaction {
  def getEmpty = Transaction(id = "random_uuid", uniqueId = "1", sequence = 0L, primaryAccountInternalId = 0,
    primaryAccountUuid = UUIDLike.empty,
    primaryUserUuid = UUIDLike.empty, primaryAccountName = "", primaryAccountNumber = "", primaryAccountType = "",
    primaryUsername = None, primaryIndividualUsersName = None,
    primaryIndividualUsersFullname = None, primaryBusinessUsersBusinessName = None, primaryBusinessUsersBrandName = None,
    secondaryAccountInternalId = 0,
    secondaryAccountUuid = UUIDLike.empty, secondaryUserUuid = UUIDLike.empty,
    secondaryAccountName = "", secondaryAccountNumber = "", direction = None, `type` = None,
    amount = None, currency = None, exchangedCurrency = None, channel = None, explanation = None,
    effectiveRate = None, costRate = None,
    status = None, instrument = None, createdAt = None, updatedAt = None,
    primaryAccountPreviousBalance = None, secondaryAccountPreviousBalance = None, provider = None)
}
