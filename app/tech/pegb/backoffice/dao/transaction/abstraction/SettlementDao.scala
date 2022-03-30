package tech.pegb.backoffice.dao.transaction.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.transaction.dto.{SettlementCriteria, SettlementFxHistoryCriteria, SettlementRecentAccountCriteria, SettlementToInsert}
import tech.pegb.backoffice.dao.transaction.entity.{Settlement, SettlementFxHistory, SettlementRecentAccount}
import tech.pegb.backoffice.dao.transaction.sql.SettlementSqlDao

@ImplementedBy(classOf[SettlementSqlDao])
trait SettlementDao extends Dao {

  def countSettlementsByCriteria(criteria: SettlementCriteria): DaoResponse[Int]

  def getSettlementsByCriteria(criteria: SettlementCriteria, orderBy: Option[OrderingSet] = None, limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[Settlement]]

  def insertSettlement(dto: SettlementToInsert): DaoResponse[Settlement]

  def getSettlementFxHistory(criteria: SettlementFxHistoryCriteria, orderBy: Option[OrderingSet] = None, limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[SettlementFxHistory]]

  def countSettlementFxHistory(criteria: SettlementFxHistoryCriteria): DaoResponse[Int]

  def getSettlementRecentAccounts(criteria: SettlementRecentAccountCriteria, limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[SettlementRecentAccount]]
}
