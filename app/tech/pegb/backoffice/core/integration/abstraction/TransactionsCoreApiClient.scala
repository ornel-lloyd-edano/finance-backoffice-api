package tech.pegb.backoffice.core.integration.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.core.integration.dto.{ReverseTxnRequest, ReversedTxnResponse, TxnCancellationRequest}
import tech.pegb.backoffice.domain.BaseService.ServiceResponse

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait TransactionsCoreApiClient {
  def getCancelTxnUrl: String
  def getReverseTxnUrl: String

  def cancelTransaction(dto: TxnCancellationRequest): Future[ServiceResponse[Unit]]

  def revertTransaction(dto: ReverseTxnRequest): Future[ServiceResponse[ReversedTxnResponse]]
}
