package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.fee.FeeProfileController
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class FeeProfileProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders
  with FeeProfileController with ProxyController {

  def createFeeProfile: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def getFeeProfile(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getFeeProfileByCriteria(
    id: Option[UUIDLike] = None,
    feeType: Option[String] = None,
    userType: Option[String] = None,
    tier: Option[String] = None,
    subscriptionType: Option[String] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    otherParty: Option[String] = None,
    instrument: Option[String] = None,
    calculationMethod: Option[String] = None,
    currencyCode: Option[String] = None,
    feeMethod: Option[String] = None,
    taxIncluded: Option[String] = None,
    partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def updateFeeProfile(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def deleteFeeProfile(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

}
