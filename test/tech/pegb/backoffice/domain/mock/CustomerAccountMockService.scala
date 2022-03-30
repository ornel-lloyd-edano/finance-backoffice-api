package tech.pegb.backoffice.domain.mock

import java.sql.Connection
import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.account.dto.{AccountCriteria, AccountToCreate}
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountNumber, AccountType}
import tech.pegb.backoffice.domain.customer.abstraction.CustomerAccount
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.util.UUIDLike

import scala.concurrent.Future

abstract class CustomerAccountMockService extends CustomerAccount {
  def openBusinessUserAccount(customerId: UUID, accountType: AccountType, currency: Currency, isMainAccount: Boolean, mainType: String, createdBy: String, createdAt: LocalDateTime)(implicit maybeTransaction: Option[Connection]): Future[ServiceResponse[Account]] = ???

  def openIndividualUserAccount(customerId: UUID, accountToCreate: AccountToCreate): Future[ServiceResponse[Account]] = ???

  def getAccounts(customerId: UUID) = ???

  def getAccountByAccountNumber(accountNumber: AccountNumber) = ???

  def getAccountByAccountName(accountName: NameAttribute) = ???

  def countAccountsByCriteria(customerId: Option[UUIDLike], isMainAccount: Option[Boolean], currency: Option[String], status: Option[String], accountType: Option[String]): Future[ServiceResponse[Long]] = ???

  def getAccountsByCriteria(
    criteria: AccountCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Account]]] = ???

  def getMainAccount(customerId: UUID) = ???

  def activateIndividualUserAccount(customerId: UUID, accntId: UUID, doneBy: String, doneAt: LocalDateTime) = ???

  def deactivateIndividualUserAccount(customerId: UUID, accntId: UUID, doneBy: String, doneAt: LocalDateTime) = ???

  def closeIndividualUserAccount(customerId: UUID, accntId: UUID, doneBy: String, doneAt: LocalDateTime) = ???

}
