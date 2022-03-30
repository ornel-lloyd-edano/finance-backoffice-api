package tech.pegb.backoffice.domain

import tech.pegb.backoffice.domain.BaseService.ServiceResponse

import scala.concurrent.Future

trait LatestVersionService[T1, T2] {

  def getLatestVersion(criteria: T1): Future[ServiceResponse[Option[T2]]]

}
