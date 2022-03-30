package tech.pegb.backoffice.domain.util.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.util.implementation.LatestVersionFactory

import scala.concurrent.Future

//TODO replace with tech.pegb.backoffice.domain.LatestVersionService
@ImplementedBy(classOf[LatestVersionFactory])
trait LatestVersionService {

  def getLatestVersion(anyCriteria: AnyRef): Future[ServiceResponse[Option[String]]]

}
