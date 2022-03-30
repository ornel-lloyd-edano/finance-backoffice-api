package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.reportsv2.controllers.ReportDefinitionController
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class ReportDefinitionProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders
  with ReportDefinitionController with ProxyController {

  def createReportDefinition: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def getReportDefinition(name: Option[String], partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def getReportDefinitionById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def updateReportDefinition(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def deleteReportDefinitionById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

}
