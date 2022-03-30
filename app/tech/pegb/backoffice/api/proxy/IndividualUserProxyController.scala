package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.customer.controllers.IndividualUserController
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class IndividualUserProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with IndividualUserController with ProxyController {

  def getIndividualUser(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def activateIndividualUser(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/activate")
  }

  def deactivateIndividualUser(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId")
  }

  def updateIndividualUser(customerId: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId")
  }

  def openIndividualUserAccount(customerId: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/accounts")
  }

  def getIndividualUserAccounts(
    customerId: UUID,
    primaryAccount: Option[Boolean],
    accountType: Option[String],
    accountNumber: Option[String],
    status: Option[String],
    currency: Option[String]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/accounts")
  }

  def getIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId")
  }

  def activateIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId/activate")
  }

  def deactivateIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId/deactivate")
  }

  def closeIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/accounts/$accountId")
  }

  def getIndividualUserWalletApplications(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/wallet_applications")
  }

  def getIndividualUserWalletApplicationByApplicationId(customerId: UUID, applicationId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/wallet_applications/$applicationId")
  }

  def approveWalletApplicationByUserId(customerId: UUID, applicationId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/wallet_applications/$applicationId/approve")
  }

  def rejectWalletApplicationByUserId(customerId: UUID, applicationId: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/wallet_applications/$applicationId/reject")
  }

  def getIndividualUsersByCriteria(
    msisdn: Option[String],
    userId: Option[UUIDLike],
    alias: Option[String],
    fullname: Option[String],
    status: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(s"$getRoute")
  }

  def getIndividualUsersDocuments(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/documents")
  }

  def getIndividualUsersDocumentByDocId(customerId: UUID, docId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/documents/$docId")
  }

  def rejectDocument(customerId: UUID, docId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/documents/$docId/reject")
  }

  def approveDocument(customerId: UUID, docId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$customerId/documents/$docId/approve")
  }

}
