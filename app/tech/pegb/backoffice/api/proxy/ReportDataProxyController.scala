package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.proxy.model.{Module, Scope}
import tech.pegb.backoffice.api.reportsv2.controllers.ReportDataController
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class ReportDataProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders
  with ReportDataController with ProxyController {

  override lazy val scope: Scope = Scope(parent = "reporting") //Note: why we violated the convention I now forgot the reason. Maybe Tanmoy still knows

  def getReportData(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

}
