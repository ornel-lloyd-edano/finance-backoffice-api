package tech.pegb.backoffice.api.proxy

import com.google.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.reportsv2.controllers.ReportResourcesControllerT
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class ReportResourcesProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders with ReportResourcesControllerT with ProxyController {

  def getAvailableReportsForUser = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

}
