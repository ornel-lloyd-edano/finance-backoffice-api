package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.customer.controllers.AccountController
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.proxy.model.Scope
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class AccountProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders with AccountController with ProxyController {

  def createAccount(): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def getAccountById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getAccountByAccountNumber(accountNumber: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/account_number/$accountNumber")
  }

  def getAccountByAccountName(accountName: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/account_name/$accountName")
  }

  def activateCustomerAccount(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id/activate")
  }

  def closeCustomerAccount(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒

    composeProxyRequest(s"$getRoute/$id/close")
  }

  def deactivateCustomerAccount(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/deactivate")
  }

  def getAccountsByCriteria(
    customerId: Option[UUIDLike],
    customerFullName: Option[String],
    anyCustomerName: Option[String],
    msisdn: Option[String],
    isMainAccount: Option[Boolean],
    currency: Option[String],
    status: Option[String],
    accountType: Option[String],
    accountNumber: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def getFloatAccountAggregations(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"floats", maybeCustomScope = Some(Scope("floats")))
  }

}
