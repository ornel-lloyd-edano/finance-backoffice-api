package tech.pegb.backoffice.domain.makerchecker.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.makerchecker.implementation.RequestCreatorImpl
import tech.pegb.backoffice.domain.makerchecker.model.MakerCheckerTask

import scala.concurrent.Future

@ImplementedBy(classOf[RequestCreatorImpl])
trait RequestCreator {

  def createRequest(makerCheckerTask: MakerCheckerTask, hostPlaceHolder: String, actualHost: String): Future[ServiceResponse[Unit]]

}
