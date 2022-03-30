package tech.pegb.backoffice.api.proxy.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import play.api.mvc.Request
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.api.proxy.ProxyRequestHandlerImpl
import tech.pegb.backoffice.api.proxy.model.Module
import tech.pegb.backoffice.domain.auth.model.BackOfficeUser

import scala.util.Try

@ImplementedBy(classOf[ProxyRequestHandlerImpl])
trait ProxyRequestHandler {

  def createRequest[T](
    user: Option[BackOfficeUser],
    internalUrl: String)(implicit request: Request[T], module: Module, requestId: UUID): Try[ProxyRequest[_]]

}
