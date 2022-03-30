package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.customer.controllers.CustomersController
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.proxy.model.{Module, Scope}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class CustomersProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with CustomersController with ProxyController {

  def getCustomerAccounts(
    id: UUID,
    primaryAccount: Option[Boolean],
    accountType: Option[String],
    accountNumber: Option[String],
    status: Option[String],
    currency: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    val customModule: Module = Module(name = "accounts")
    val customScope: Scope = Scope(parent = "accounts")
    composeProxyRequest(s"$getRoute/$id/accounts", Some(customScope), Some(customModule))
  }

  def getCustomerAccountById(
    id: UUID,
    accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    val customModule: Module = Module(name = "accounts")
    val customScope: Scope = Scope(parent = "accounts")
    composeProxyRequest(s"$getRoute/$id/accounts/$accountId", Some(customScope), Some(customModule))
  }

  def activateCustomerAccount(customerId: UUID, accountId: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒

    val customModule: Module = Module(name = "accounts")
    val customScope: Scope = Scope(parent = "accounts")
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId/activate", Some(customScope), Some(customModule))
  }

  def deactivateCustomerAccount(customerId: UUID, accountId: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    val customModule: Module = Module(name = "accounts")
    val customScope: Scope = Scope(parent = "accounts")
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId/deactivate", Some(customScope), Some(customModule))
  }

  def closeCustomerAccount(customerId: UUID, accountId: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    val customModule: Module = Module(name = "accounts")
    val customScope: Scope = Scope(parent = "accounts")
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId", Some(customScope), Some(customModule))
  }

  def getTransactions(
    customerId: UUID,
    accountId: UUID,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    `type`: Option[String],
    channel: Option[String],
    status: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    val customModule: Module = Module(name = "transactions")
    val customScope: Scope = Scope(parent = "transactions")
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId/transactions", Some(customScope), Some(customModule))
  }

  def getPaymentOptionsTransactions(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    val customModule: Module = Module(name = "transactions")
    val customScope: Scope = Scope(parent = "transactions")
    composeProxyRequest(s"$getRoute/$customerId/payment_options", Some(customScope), Some(customModule))
  }

  def getUserByCriteria(
    msisdn: Option[String],
    userId: Option[UUIDLike],
    alias: Option[String],
    fullname: Option[String],
    status: Option[String],
    anyName: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def getUser(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

}
