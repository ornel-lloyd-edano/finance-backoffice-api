package tech.pegb.backoffice.core.integration.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.domain.BaseService

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait ManualTransactionCoreApiClient extends BaseService {

  protected val createManualTxnUrl: String
  protected val createManualTxnFxUrl: String

  def createManualTransaction(settlementId: Int, dto: Seq[ManualTxnLinesToCreateCoreDto], isFx: Boolean): Future[ServiceResponse[Unit]]
}

trait ManualTxnLinesToCreateCoreDto
