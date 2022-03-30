package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.customer.controllers.{BusinessUserController, CustomerExternalAccountsController, CustomerTxnConfigController}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class BusinessUserProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with CustomerExternalAccountsController
  with CustomerTxnConfigController
  with BusinessUserController with ProxyController {

  def getVelocityPortalUsers(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/velocity_portal_users")
  }

  def getVelocityPortalUserById(userId: UUID, vpUserId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/velocity_portal_users/$vpUserId")
  }

  def resetVelocityPortalPin(userId: UUID, vpUserId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/velocity_portal_users/$vpUserId/reset_pin")
  }

  def getContacts(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/contacts")
  }

  def getAddress(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/addresses")
  }

  def getContactsById(userId: UUID, contactId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/contacts/$contactId")
  }

  def getAddressById(userId: UUID, addressId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/address/$addressId")
  }

  def createContact(userId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/contacts")
  }

  def updateContact(userId: UUID, contactId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/contacts/$contactId")
  }

  def createAddress(userId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/addresses")
  }

  def updateAddress(userId: UUID, addressId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$userId/addresses/$addressId")
  }

  def getCustomerExternalAccount(customerId: UUID, externalAccountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/external_accounts/$externalAccountId")
  }

  def getCustomerExternalAccountsByCriteria(
    customerId: UUID,
    externalAccountId: Option[UUIDLike],
    currency: Option[String],
    providerName: Option[String],
    accountNumber: Option[String],
    accountHolder: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/external_accounts")
  }

  def createCustomerExternalAccount(customerId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/external_accounts")
  }

  def updateCustomerExternalAccount(customerId: UUID, externalAccountId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/external_accounts/$externalAccountId")
  }

  def deleteCustomerExternalAccount(customerId: UUID, externalAccountId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/external_accounts/$externalAccountId")
  }

  def getCustomerTxnConfig(customerId: UUID, txnConfId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/txn_configs/$txnConfId")
  }

  def getCustomerTxnConfigByCriteria(
    customerId: UUID,
    txnConfId: Option[UUIDLike],
    currency: Option[String],
    transactionType: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/txn_configs")
  }

  def createCustomerTxnConfig(customerId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/txn_configs")
  }

  def updateCustomerTxnConfig(customerId: UUID, txnConfId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/txn_configs/$txnConfId")
  }

  def deleteCustomerTxnConfig(customerId: UUID, txnConfId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/txn_configs/$txnConfId")
  }
}
