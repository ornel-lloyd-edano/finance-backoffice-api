package tech.pegb.backoffice.core.integration.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.domain.BaseService

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait FeeProfileCoreApiClient extends BaseService {

  def notifyFeeProfileUpdated(feeProfileId: Int)(implicit requestId: UUID): Future[ServiceResponse[Unit]]

}
