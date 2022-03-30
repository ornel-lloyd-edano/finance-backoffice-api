package tech.pegb.backoffice.domain.customer.abstraction

import java.sql.Connection
import java.time.LocalDateTime
import java.util.{Currency, UUID}

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.account.dto.{AccountCriteria, AccountToCreate}
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType, AccountNumber, AccountType}
import tech.pegb.backoffice.domain.customer.implementation.CustomerAccountService
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerAccountService])
trait CustomerAccount {

  def openBusinessUserAccount(customerId: UUID, accountType: AccountType, currency: Currency, isMainAccount: Boolean, mainType: AccountMainType, createdBy: String, createdAt: LocalDateTime)(implicit maybeTransaction: Option[Connection] = None): Future[ServiceResponse[Account]]

  def openIndividualUserAccount(customerId: UUID, accountToCreate: AccountToCreate): Future[ServiceResponse[Account]]

  def getAccounts(customerId: UUID): Future[ServiceResponse[Set[Account]]]

  def getAccountByAccountNumber(accountNumber: AccountNumber): Future[ServiceResponse[Account]]

  def getAccountByAccountName(accountName: NameAttribute): Future[ServiceResponse[Account]]

  def getAccountsByCriteria(
    criteria: AccountCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Account]]]

  def getMainAccount(customerId: UUID): Future[ServiceResponse[Account]] //not Option[Account] because it is considered a business error if customer does not have main account

  def activateIndividualUserAccount(customerId: UUID, accountId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[Account]]

  def deactivateIndividualUserAccount(customerId: UUID, accountId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[Account]]

  def closeIndividualUserAccount(customerId: UUID, accountId: UUID, doneBy: String, doneAt: LocalDateTime): Future[ServiceResponse[Account]]

}
