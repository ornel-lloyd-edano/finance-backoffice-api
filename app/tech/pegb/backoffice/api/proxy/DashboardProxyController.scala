package tech.pegb.backoffice.api.proxy

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.aggregations.controllers.{FloatController, RevenueController}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders, Routable}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class DashboardProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with FloatController
  with RevenueController
  with Routable
  with ProxyController {

  override def getRoute: String = "dashboards"

  val RevenueRoute = "revenue"
  val FloatRoute = "floats"

  def getTotalAggregations(currencyCode: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$FloatRoute/totals")
  }

  def getInstitutionStats(currencyCode: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$FloatRoute/institutions")
  }

  def getInstitutionTrendsGraph(
    institution: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    frequency: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$FloatRoute/institutions/$institution/trends")
  }

  def updatePercentage(institution: String): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$FloatRoute/institutions/$institution")
  }

  def getAllRevenue(currencyCode: String, dateFrom: Option[LocalDateTimeFrom], dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$RevenueRoute")
  }

  def getRevenueByAggregationType(aggregation: String, currencyCode: String, dateFrom: Option[LocalDateTimeFrom], dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$RevenueRoute/aggregation/$aggregation")
  }

  def getTransactionTotals(currencyCode: String, dateFrom: Option[LocalDateTimeFrom], dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$RevenueRoute/transaction_totals")
  }
}
