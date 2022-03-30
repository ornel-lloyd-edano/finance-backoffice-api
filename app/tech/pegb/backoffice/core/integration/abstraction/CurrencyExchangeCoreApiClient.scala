package tech.pegb.backoffice.core.integration.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.domain.BaseService

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait CurrencyExchangeCoreApiClient extends BaseService {
  def updateFxStatus(
    id: Long,
    status: String,
    reason: String,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[Unit]]

  def batchUpdateFxStatus(
    idSeq: Seq[Long],
    status: String,
    reason: String,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime],
    rates: Map[String, BigDecimal] = Map.empty)(
    implicit
    requestId: UUID): Future[ServiceResponse[Unit]]

  def notifySpreadUpdated(spreadId: Int)(implicit requestId: UUID): Future[ServiceResponse[Unit]]
}
