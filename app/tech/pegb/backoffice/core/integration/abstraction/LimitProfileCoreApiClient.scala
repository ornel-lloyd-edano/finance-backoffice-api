package tech.pegb.backoffice.core.integration.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.domain.BaseService

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait LimitProfileCoreApiClient extends BaseService {
  def notifyLimitProfileUpdated(limitId: Int)(implicit requestId: UUID): Future[ServiceResponse[Unit]]
}
