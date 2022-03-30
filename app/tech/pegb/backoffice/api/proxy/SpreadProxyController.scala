package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.currencyexchange.SpreadsController
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.proxy.model.{Module, Scope}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class SpreadProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with SpreadsController with ProxyController {

  def getSpread(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getCurrencyExchangeSpread(fxId: UUID, id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    val customModule = Module(name = "currency_exchanges")
    val customScope = Scope(parent = "currency_exchanges")
    composeProxyRequest(s"currency_exchanges/$fxId/spreads/$id", Some(customScope), Some(customModule))
  }

  def getSpreadsByCriteria(
    id: Option[UUIDLike],
    currencyExchangeId: Option[UUIDLike],
    currency: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    institution: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def createSpreads(): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }
}
