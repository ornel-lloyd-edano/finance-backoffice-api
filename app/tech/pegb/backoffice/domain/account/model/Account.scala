package tech.pegb.backoffice.domain.account.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.account.model.AccountAttributes._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, NameAttribute}
import tech.pegb.backoffice.domain.financial.Implicits._
import tech.pegb.backoffice.util.Constants

case class Account(
    id: UUID,
    customerId: UUID,
    anyCustomerName: Option[String],
    userName: Option[String],
    customerName: Option[String],
    msisdn: Option[Msisdn],
    accountNumber: AccountNumber,
    accountName: NameAttribute,
    accountType: AccountType,
    isMainAccount: Boolean,
    currency: Currency,
    balance: BigDecimal,
    blockedBalance: BigDecimal,
    dailyTotalTransactionAmount: Option[BigDecimal],
    lastDayBalance: Option[BigDecimal],
    accountStatus: AccountStatus,
    lastTransactionAt: Option[LocalDateTime],
    mainType: AccountMainType,
    createdAt: LocalDateTime,
    createdBy: Option[String],
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

  val availableBalance: BigDecimal = (balance - blockedBalance).toFinancial
  val isLiability: Boolean = mainType == AccountMainType(Constants.Liability)

}

object Account {
  val BLOCKED = "deactivated"
  val FROZEN = "frozen"
  val ACTIVE = "active"
  val CLOSED = "closed"

  def getEmpty = Account(id = UUID.randomUUID(), customerId = UUID.randomUUID(),
    userName = None,
    anyCustomerName = None,
    customerName = None, msisdn = None, accountNumber = AccountNumber("account number"), accountName = NameAttribute("account name"),
    accountType = AccountType("account type"), isMainAccount = true, currency = Currency.getInstance("AED"),
    balance = BigDecimal(0), blockedBalance = BigDecimal(0), dailyTotalTransactionAmount = None, lastDayBalance = None,
    accountStatus = AccountStatus("status"), lastTransactionAt = None, mainType = AccountMainType(Constants.Liability),
    createdAt = LocalDateTime.now, createdBy = Some(""), updatedBy = None, updatedAt = None)
}
