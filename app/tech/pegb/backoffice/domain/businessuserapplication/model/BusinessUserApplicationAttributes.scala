package tech.pegb.backoffice.domain.businessuserapplication.model

import java.util.Currency

import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountType}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.util.Implicits._

object BusinessUserApplicationAttributes {

  //base table fields
  case class BusinessCategory(underlying: String) {
    assert(underlying.hasSomething, "empty business category")
  }

  case class ApplicationStage(underlying: String) {
    assert(underlying.hasSomething, "empty application stage")
  }

  case class ApplicationStatus(underlying: String) {
    assert(underlying.hasSomething, "empty application status")
  }

  case class RegistrationNumber(underlying: String) {
    assert(underlying.hasSomething, "empty registration number")
  }

  case class TaxNumber(underlying: String) {
    assert(underlying.hasSomething, "empty tax number")
  }

  case class TransactionConfig(
      transactionType: TransactionType,
      currency: Currency)

  case class AccountConfig(
      accountType: AccountType,
      accountName: NameAttribute,
      currency: Currency,
      isDefault: Boolean)

  case class ExternalAccount(
      provider: NameAttribute,
      accountNumber: AccountNumber,
      accountHolder: NameAttribute,
      currency: Currency)
}
