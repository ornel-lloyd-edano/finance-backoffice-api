package tech.pegb.backoffice.api.proxy

import java.time.LocalDate
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, _}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.application.WalletApplicationController
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class WalletApplicationProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders with ProxyController with WalletApplicationController {

  def getWalletApplication(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getWalletApplicationsByCriteria(
    status: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    msisdn: Option[String],
    nationalId: Option[String],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    orderBy: Option[String],
    limit: Option[Int] = None,
    offset: Option[Int] = None): Action[AnyContent] =
    LoggedAsyncAction { implicit ctx ⇒
      composeProxyRequest(getRoute)
    }

  def getDocumentsByWalletApplication(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/documents")
  }

  def approveWalletApplication(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/approve")
  }

  def rejectWalletApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/reject")
  }

}
