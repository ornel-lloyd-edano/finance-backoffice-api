package tech.pegb.backoffice.core.integration.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.domain.BaseService.ServiceResponse

import scala.concurrent.Future

@ImplementedBy(classOf[CoreApiClient])
trait SavingOptionsCoreApiClient {

  def getDeactivateSavingGoalUrl: String
  def getDeactivateAutoDeductUrl: String
  def getDeactivateRoundupUrl: String

  def deactivateSavingGoal(goalId: Long)(implicit requestId: UUID): Future[ServiceResponse[Unit]]

  def deactivateAutoDeductSaving(goalId: Long)(implicit requestId: UUID): Future[ServiceResponse[Unit]]

  def deactivateRoundUpSaving(goalId: Long)(implicit requestId: UUID): Future[ServiceResponse[Unit]]

}
