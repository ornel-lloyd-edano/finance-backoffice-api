package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.makerchecker.MakerCheckerMgmtController
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class MakerCheckerProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders
  with MakerCheckerMgmtController with ProxyController {

  def getTask(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getTasksByCriteria(
    moduleName: Option[String],
    status: Option[String],
    createdAtDateFrom: Option[LocalDateTimeFrom],
    createdAtDateTo: Option[LocalDateTimeTo],
    isReadOnly: Option[Boolean],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int] = None,
    offset: Option[Int] = None): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def approveTask(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id/approve")
  }

  def rejectTask(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id/reject")
  }

  def createTask: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    Future.successful(Forbidden)
  }

}

