package tech.pegb.backoffice.domain.transaction.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.transaction.dto.{TxnCancellation, TxnReversal}
import tech.pegb.backoffice.domain.transaction.dto._
import tech.pegb.backoffice.domain.transaction.implementation.TransactionMgmtService
import tech.pegb.backoffice.domain.transaction.model.Transaction

import scala.concurrent.Future

@ImplementedBy(classOf[TransactionMgmtService])
trait TransactionManagement extends TransactionService {

  //TODO unify later
  def cancelTransaction(dto: TxnCancellation): Future[ServiceResponse[Seq[Transaction]]]

  def revertTransaction(dto: TxnReversal): Future[ServiceResponse[Seq[Transaction]]]

  def getTxnReversalMetadata(reversedTxnId: String): ServiceResponse[Option[ReasonMetadata]]

  def getTxnCancellationMetadata(cancelledTxnId: String): ServiceResponse[Option[ReasonMetadata]]
}
