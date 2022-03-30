package tech.pegb.backoffice.domain.account.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.account.dto.{AccountCriteria, AccountToCreate}
import tech.pegb.backoffice.domain.account.model.{Account, FloatAccountAggregation}
import tech.pegb.backoffice.domain.account.implementation.AccountMgmtService
import tech.pegb.backoffice.domain.account.model.AccountAttributes.AccountNumber
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria

import scala.concurrent.Future

@ImplementedBy(classOf[AccountMgmtService])
trait AccountManagement {

  def createAccount(accountToCreate: AccountToCreate, expectedUserType: Option[String] = None): Future[ServiceResponse[Account]]

  def getAccountById(id: UUID): Future[ServiceResponse[Account]]

  def getAccountByAccountNumber(accountNumber: AccountNumber): Future[ServiceResponse[Account]]

  def getAccountByAccountName(accountName: NameAttribute): Future[ServiceResponse[Account]]

  def getAccountsByCriteria(
    criteria: AccountCriteria, orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Account]]]

  def countAccountsByCriteria(criteria: AccountCriteria): Future[ServiceResponse[Int]]

  def blockAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime] = None): Future[ServiceResponse[Account]]

  def freezeAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime] = None): Future[ServiceResponse[Account]]

  def activateAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime] = None): Future[ServiceResponse[Account]]

  def deleteAccount(id: UUID, doneBy: String, doneAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime] = None): Future[ServiceResponse[Account]]

  def getBalance(id: UUID): Future[ServiceResponse[BigDecimal]]

  def withdrawAmount(id: UUID, amount: BigDecimal): Future[ServiceResponse[Account]]

  def depositAmount(id: UUID, amount: BigDecimal): Future[ServiceResponse[Account]]

  def transferAmount(sourceAccountId: UUID, destinationAccountId: UUID, amount: BigDecimal): Future[ServiceResponse[Unit]]

  def executeOnFlyAggregation(
    transactionCriteria: TransactionCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[FloatAccountAggregation]]]

}
