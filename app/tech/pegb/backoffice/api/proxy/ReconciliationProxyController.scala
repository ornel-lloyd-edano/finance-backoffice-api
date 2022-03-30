package tech.pegb.backoffice.api.proxy

import java.time.LocalDate

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.recon.controllers.ReconciliationController
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class ReconciliationProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders
  with ReconciliationController with ProxyController {

  def getReconciliationSummaryById(id: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getInternalRecon(
    maybeId: Option[String],
    maybeUserId: Option[String],
    maybeAnyCustomerName: Option[String],
    maybeAccountNumber: Option[String],
    maybeAccountType: Option[String],
    maybeStatus: Option[String],
    maybeStartReconDate: Option[LocalDateTimeFrom],
    maybeEndReconDate: Option[LocalDateTimeTo],
    maybeOrderBy: Option[String],
    partialMatch: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def getInternalReconIncidents(
    maybeReconId: Option[String],
    maybeAccountNumber: Option[String],
    maybeStartReconDate: Option[LocalDateTimeFrom],
    maybeEndReconDate: Option[LocalDateTimeTo],
    maybeOrderBy: Option[String],
    partialMatch: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/incidents")
  }

  def updateReconStatus(id: String): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def externalRecon(thirdParty: String, source: Option[String], startDate: LocalDate, endDate: LocalDate) = ???

  def getTxnsForThirdPartyRecon(thirdParty: String, startDate: LocalDate, endDate: LocalDate) = ???
}
