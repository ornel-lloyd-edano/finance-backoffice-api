package tech.pegb.backoffice.domain.transaction.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.LatestVersionService
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.dto.{TxnConfigCriteria, TxnConfigToUpdate, TxnConfigToCreate}
import tech.pegb.backoffice.domain.transaction.implementation.TransactionConfigMgmtService
import tech.pegb.backoffice.domain.transaction.model.TxnConfig

import scala.concurrent.Future

@ImplementedBy(classOf[TransactionConfigMgmtService])
trait TransactionConfigManagement extends LatestVersionService[TxnConfigCriteria, TxnConfig] {

  def createTxnConfig(dto: TxnConfigToCreate): Future[ServiceResponse[TxnConfig]]

  def getTxnConfigByCriteria(criteria: TxnConfigCriteria, orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[TxnConfig]]]

  def getLatestVersion(criteria: TxnConfigCriteria): Future[ServiceResponse[Option[TxnConfig]]]

  def count(criteria: TxnConfigCriteria): Future[ServiceResponse[Int]]

  def updateTxnConfig(criteria: TxnConfigCriteria, dto: TxnConfigToUpdate): Future[ServiceResponse[TxnConfig]]

  def deleteTxnConfig(criteria: TxnConfigCriteria, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]

}
