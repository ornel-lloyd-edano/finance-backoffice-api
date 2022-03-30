package tech.pegb.backoffice.domain.transaction.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.Identifiable
import tech.pegb.backoffice.util.UUIDLike

case class Transaction(
    id: String,
    uniqueId: String,
    sequence: Long,
    primaryAccountId: UUID,
    primaryAccountName: String,
    primaryAccountNumber: String,
    primaryAccountType: String,
    primaryAccountCustomerName: Option[String] = None,
    secondaryAccountId: UUID,
    secondaryAccountName: String,
    secondaryAccountNumber: String,
    direction: String,
    `type`: String,
    amount: BigDecimal,
    currency: Currency,
    exchangedCurrency: Option[Currency] = None,
    channel: String,
    explanation: Option[String],
    effectiveRate: Option[BigDecimal],
    costRate: Option[BigDecimal],
    status: TransactionStatus,
    instrument: Option[String],
    createdAt: LocalDateTime,
    updatedAt: Option[LocalDateTime] = None,
    primaryAccountPreviousBalance: Option[BigDecimal] = None) extends Identifiable

object Transaction {
  def getEmpty = Transaction(id = "random_uuid", uniqueId = "1", sequence = 0L, primaryAccountId = UUIDLike.empty,
    primaryAccountName = "", primaryAccountNumber = "", primaryAccountType = "",
    primaryAccountCustomerName = None, secondaryAccountId = UUIDLike.empty,
    secondaryAccountName = "", secondaryAccountNumber = "", direction = "", `type` = "",
    amount = BigDecimal(0), currency = Currency.getInstance("KES"), exchangedCurrency = None,
    channel = "", explanation = None, effectiveRate = None, costRate = None,
    status = TransactionStatus("success"), instrument = None,
    createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0), updatedAt = None,
    primaryAccountPreviousBalance = None)
}
