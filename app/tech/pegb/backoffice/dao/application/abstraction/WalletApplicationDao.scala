package tech.pegb.backoffice.dao.application.abstraction

import java.sql.Connection
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.application.dto.{WalletApplicationCriteria, WalletApplicationToCreate, WalletApplicationToUpdate}
import tech.pegb.backoffice.dao.application.entity.WalletApplication
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[WalletApplicationSqlDao])
trait WalletApplicationDao extends Dao {

  def getWalletApplicationByUUID(id: UUID): DaoResponse[Option[WalletApplication]]

  def getWalletApplicationByUserUuid(userUuid: UUID): DaoResponse[Set[WalletApplication]]

  def getWalletApplicationByInternalId(id: Int): DaoResponse[Option[WalletApplication]]

  def getWalletApplicationsByCriteria(criteria: WalletApplicationCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[WalletApplication]]

  def countWalletApplicationsByCriteria(criteria: WalletApplicationCriteria): DaoResponse[Int]

  def updateWalletApplication(id: UUID, walletApplicationToUpdate: WalletApplicationToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[WalletApplication]]

  def insertWalletApplication(walletApplicationToCreate: WalletApplicationToCreate): DaoResponse[WalletApplication]

  def getInternalIdByUUID(id: UUID)(implicit cxn: Connection): DaoResponse[Int]

}
