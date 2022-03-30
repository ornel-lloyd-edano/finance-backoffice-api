package tech.pegb.backoffice.domain.transaction.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.dto.{ManualTxnCriteria, ManualTxnToCreate, SettlementFxHistoryCriteria, SettlementRecentAccountCriteria}
import tech.pegb.backoffice.domain.transaction.implementation.ManualTxnMgmtService
import tech.pegb.backoffice.domain.transaction.model.{ManualTransaction, SettlementFxHistory, SettlementRecentAccount}

import scala.concurrent.Future

@ImplementedBy(classOf[ManualTxnMgmtService])
trait ManualTransactionManagement {

  def getManualTransactionsByCriteria(
    isGrouped: Boolean,
    criteria: ManualTxnCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[ManualTransaction]]]

  def countManualTransactionsByCriteria(isGrouped: Boolean, criteria: ManualTxnCriteria): Future[ServiceResponse[Int]]

  def createManualTransactions(manualTxn: ManualTxnToCreate): Future[ServiceResponse[ManualTransaction]]

  def countSettlementFxHistory(criteria: SettlementFxHistoryCriteria): Future[ServiceResponse[Int]]

  def getSettlementFxHistory(
    criteria: SettlementFxHistoryCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[SettlementFxHistory]]]

  def getSettlementRecentAccount(
    criteria: SettlementRecentAccountCriteria,
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[SettlementRecentAccount]]]
}
