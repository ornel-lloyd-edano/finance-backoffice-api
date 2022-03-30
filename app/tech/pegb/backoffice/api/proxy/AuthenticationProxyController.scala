package tech.pegb.backoffice.api.proxy

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import tech.pegb.backoffice.api.auth.controllers.AuthenticationController
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.abstraction.ProxyRequestHandler
import tech.pegb.backoffice.api.proxy.model.{Module, Scope}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class AuthenticationProxyController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    proxyResponseHandler: ProxyResponseHandler,
    authenticationMiddleware: AuthenticationMiddleware,
    authorizationMiddleware: AuthorizationMiddleware,
    makerCheckerMiddleware: MakerCheckerMiddleware,
    proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders with AuthenticationController {

  import ApiController._

  private val scopeAndModule = "admin"

  implicit val module: Module = Module(name = scopeAndModule)
  implicit val scope: Scope = Scope(parent = scopeAndModule)

  implicit val executionContext: ExecutionContext = executionContexts.genericOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def login = ???

  def updatePassword = ???

  def resetPassword: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      backOfficeUser ← EitherT(authenticationMiddleware.getBackOfficeUser)
      _ ← EitherT.fromEither[Future](authorizationMiddleware.checkPermissions(backOfficeUser, ctx.method))

      generalProxyRequest ← EitherT.fromEither[Future](proxyRequestHandler.createRequest(Some(backOfficeUser), "reset_password").toEither)
        .leftMap(e ⇒ s"could not create proxy request, reason: ${e.getMessage}".asInvalidRequestApiError)

      proxyRequest ← EitherT.fromEither[Future] {
        if (makerCheckerMiddleware.isValidMakerCheckerRequest) makerCheckerMiddleware.createMakerCheckerRequest(generalProxyRequest).toEither
        else Right(generalProxyRequest)
      }.leftMap(e ⇒ s"could not create maker checker request, reason: ${e.getMessage}".asInvalidRequestApiError)

      response ← EitherT(proxyResponseHandler.handleApiProxyResponse(proxyRequest))

    } yield response).value.map(_.fold(makeApiErrorResponse, identity))
  }

  def validateResetPasswordByToken(token: String) = ???

  def updateResetPassword = ???

  def getStatus = ???

  def validateToken = ???
}
