package tech.pegb.backoffice.core.integration.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.core.integration.dto.BusinessUserCreateResponse
import tech.pegb.backoffice.domain.BaseService

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait BusinessUserCoreApiClient extends BaseService {
  def createBusinessUserApplication(applicationId: Int, createdBy: String): Future[ServiceResponse[BusinessUserCreateResponse]]

  def resetVelocityPortalUserPin(vpUserId: Int, reason: String, updatedBy: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]]
}
