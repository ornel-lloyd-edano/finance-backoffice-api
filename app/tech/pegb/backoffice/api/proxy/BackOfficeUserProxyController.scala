package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.controllers.BackOfficeUserController
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class BackOfficeUserProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with BackOfficeUserController with ProxyController {

  def createBackOfficeUser(reactivate: Option[Boolean]): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def getBackOfficeUserById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getBackOfficeUsers(userName: Option[String], firstName: Option[String], lastName: Option[String],
    email: Option[String], phoneNumber: Option[String], roleId: Option[String],
    businessUnitId: Option[String], scopeId: Option[String], partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def updateBackOfficeUser(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def deleteBackOfficeUser(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

}
