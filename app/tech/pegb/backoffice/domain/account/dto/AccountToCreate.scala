package tech.pegb.backoffice.domain.account.dto

import java.time.{LocalDateTime, ZoneId}
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType, AccountNumber, AccountStatus, AccountType}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.util.Constants

case class AccountToCreate(
    customerId: UUID,
    accountNumber: Option[AccountNumber],
    accountName: Option[NameAttribute],
    accountType: AccountType,
    isMainAccount: Boolean,
    currency: Currency,
    initialBalance: Option[BigDecimal],
    accountStatus: Option[AccountStatus],
    mainType: AccountMainType,
    createdBy: String,
    createdAt: LocalDateTime)

object AccountToCreate {
  val empty = new AccountToCreate(
    customerId = UUID.randomUUID(),
    accountNumber = Option(AccountNumber("some account number")),
    accountName = Option(NameAttribute("some account name")),
    accountType = AccountType("some account type"),
    isMainAccount = true,
    currency = Currency.getInstance("USD"),
    initialBalance = None,
    accountStatus = Option(AccountStatus("some account status")),
    mainType = AccountMainType(Constants.Liability),
    createdBy = "",
    createdAt = LocalDateTime.now(ZoneId.of("UTC")))
}
