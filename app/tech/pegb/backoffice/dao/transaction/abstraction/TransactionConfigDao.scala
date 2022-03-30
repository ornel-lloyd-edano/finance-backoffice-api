package tech.pegb.backoffice.dao.transaction.abstraction

import java.sql.Connection
import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.transaction.dto.{TxnConfigCriteria, TxnConfigToCreate, TxnConfigToUpdate}
import tech.pegb.backoffice.dao.transaction.entity.TxnConfig
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.transaction.sql.TransactionConfigSqlDao

@ImplementedBy(classOf[TransactionConfigSqlDao])
trait TransactionConfigDao {

  def getTxnConfigById(id: Int)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[TxnConfig]]

  def getTxnConfigByCriteria(
    criteria: TxnConfigCriteria,
    orderBy: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None)(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[TxnConfig]]

  def countTxnConfig(criteria: TxnConfigCriteria)(implicit txnConn: Option[Connection] = None): DaoResponse[Int]

  def insertTxnConfig(dto: TxnConfigToCreate)(implicit txnConn: Option[Connection] = None): DaoResponse[TxnConfig]

  def updateTxnConfig(criteria: TxnConfigCriteria, dto: TxnConfigToUpdate)(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[TxnConfig]]

  def deleteTxnConfig(criteria: TxnConfigCriteria, lastUpdatedAt: Option[LocalDateTime])(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Unit]]
}
