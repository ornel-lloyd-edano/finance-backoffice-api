package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.customer.controllers.{ExternalAccountsController}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class ExternalAccountProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with ExternalAccountsController with ProxyController {

  def getExternalAccount(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getExternalAccountsByCriteria(
    customerId: Option[UUIDLike],
    customerName: Option[String],
    currency: Option[String],
    providerName: Option[String],
    accountNumber: Option[String],
    accountHolder: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def createExternalAccount: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def updateExternalAccount(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def deleteExternalAccount(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }
}
