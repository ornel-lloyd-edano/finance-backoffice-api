package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.model._
import tech.pegb.backoffice.api.notification.NotificationManagementController
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.proxy.model.{Module, Scope}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class NotificationMgmtProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents)
  with RequiredHeaders

  with NotificationManagementController with ProxyController {

  def createNotificationTemplate: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def deactivateNotificationTemplate(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id/deactivate")
  }

  def activateNotificationTemplate(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id/activate")
  }

  def updateNotificationTemplate(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id")
  }

  def getNotificationTemplatesByCriteria(
    id: Option[UUIDLike],
    name: Option[String],
    channel: Option[String],
    createdAtFrom: Option[LocalDateTimeFrom],
    createdAtTo: Option[LocalDateTimeTo],
    isActive: Option[Boolean],
    partialMatchFields: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def getNotificationsByCriteria(
    id: Option[UUIDLike],
    templateId: Option[UUIDLike],
    userId: Option[UUIDLike],
    operationId: Option[String],
    channel: Option[String],
    title: Option[String],
    content: Option[String],
    address: Option[String],
    status: Option[String],
    createdAtFrom: Option[LocalDateTimeFrom],
    createdAtTo: Option[LocalDateTimeTo],
    partialMatchFields: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    val route = "notifications"
    val module: Module = Module(name = route)
    val scope: Scope = Scope(parent = route)

    composeProxyRequest(s"$route", Some(scope), Some(module))
  }

  def getNotificationTemplateById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id")
  }

  def getNotificationById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    val route = "notifications"
    val module: Module = Module(name = route)
    val scope: Scope = Scope(parent = route)

    composeProxyRequest(s"$route/$id", Some(scope), Some(module))
  }
}
