package tech.pegb.backoffice.api.proxy

import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.transaction.ManualTransactionController
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

class ManualTransactionProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with ManualTransactionController with ProxyController {

  def getManualTransactions(
    id: Option[UUIDLike],
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def createManualTransaction: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def getSettlementFxHistory(
    provider: Option[String],
    fromCurrency: Option[String],
    toCurrency: Option[String],
    dateFrom: Option[LocalDateTimeFrom],
    dateTimeTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/currency_exchange_history")
  }

  def getSettlementRecentAccount(limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/frequently_used_accounts")
  }
}
