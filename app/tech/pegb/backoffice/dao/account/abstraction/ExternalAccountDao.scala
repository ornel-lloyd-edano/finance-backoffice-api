package tech.pegb.backoffice.dao.account.abstraction

import java.sql.Connection
import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.dao.account.sql.ExternalAccountSqlDao
import tech.pegb.backoffice.dao.account.entity.ExternalAccount
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[ExternalAccountSqlDao])
trait ExternalAccountDao {

  def getExternalAccountById(id: Int)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[ExternalAccount]]

  def getExternalAccountByCriteria(
    criteria: ExternalAccountCriteria,
    orderBy: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None)(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[ExternalAccount]]

  def countExternalAccount(criteria: ExternalAccountCriteria)(implicit txnConn: Option[Connection] = None): DaoResponse[Int]

  def insertExternalAccount(dto: ExternalAccountToCreate)(implicit txnConn: Option[Connection] = None): DaoResponse[ExternalAccount]

  def updateExternalAccount(criteria: ExternalAccountCriteria, dto: ExternalAccountToUpdate)(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[ExternalAccount]]

  def deleteExternalAccount(criteria: ExternalAccountCriteria, lastUpdatedAt: Option[LocalDateTime])(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Unit]]
}
