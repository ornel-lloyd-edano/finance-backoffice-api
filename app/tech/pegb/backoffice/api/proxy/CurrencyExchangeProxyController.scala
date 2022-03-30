package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.currencyexchange.CurrencyExchangeController
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class CurrencyExchangeProxyController @Inject() (
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
  with CurrencyExchangeController with ProxyController {

  def getCurrencyExchangeByCriteria(
    id: Option[UUIDLike],
    currencyCode: Option[String],
    baseCurrency: Option[String],
    provider: Option[String],
    status: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(getRoute)
  }

  def getCurrencyExchange(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getCurrencyExchangeSpreads(
    currencyExchangeId: UUID,
    transactionType: Option[String],
    channel: Option[String],
    institution: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$currencyExchangeId/spreads")
  }

  def activateFX(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/activate")
  }

  def deactivateFX(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/deactivate")
  }

  def createSpreads(currencyExchangeId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$currencyExchangeId/spreads")
  }

  def updateCurrencyExchangeSpread(spreadId: UUID, fxId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$fxId/spreads/$spreadId")
  }

  def deleteCurrencyExchangeSpread(spreadId: UUID, fxId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$fxId/spreads/$spreadId")
  }

  def batchActivateFX: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/activate")
  }

  def batchDeactivateFX: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/deactivate")
  }
}
