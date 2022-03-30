package tech.pegb.backoffice.dao.account.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.account.dto.{AccountCriteria, AccountToInsert, AccountToUpdate}
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountStatus
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[AccountSqlDao])
trait AccountDao extends Dao {
  def getAccount(id: String): DaoResponse[Option[Account]]

  def getAccountsByInternalIds(ids: Set[Int]): DaoResponse[Set[Account]]

  def getAccountStatuses: DaoResponse[Set[AccountStatus]]

  def getAccountsByUserId(userId: String): DaoResponse[Set[Account]]

  def getAccountByAccNum(accountNumber: String): DaoResponse[Option[Account]]

  def getAccountByAccountName(accountName: String): DaoResponse[Option[Account]]

  def getMainAccountByUserId(userId: String): DaoResponse[Option[Account]]

  def getAccountsByCriteria(criteria: Option[AccountCriteria], orderBy: Option[OrderingSet] = None, limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[Account]]

  def countTotalAccountsByCriteria(criteria: AccountCriteria): DaoResponse[Int]

  def insertAccount(account: AccountToInsert)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Account]

  def updateAccount(uuid: String, account: AccountToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[Account]]

  def updateAccountByCriteria(criteria: AccountCriteria, account: AccountToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Int]

  def updateAccountByAccountNumber(accountNumber: String, accountToUpdate: AccountToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[Account]]

  def deleteAccount(uuid: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Boolean]

  def deleteAccountByCriteria(criteria: AccountCriteria)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Int]
}
