package tech.pegb.backoffice.api.proxy.abstraction

import java.util.UUID

import cats.data._
import cats.implicits._
import play.api.mvc.{Request, Result}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.proxy.ProxyResponseHandler
import tech.pegb.backoffice.api.proxy.model.{Module, Scope}
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

trait ProxyController {
  this: Routable with RequiredHeaders with ApiController ⇒

  import ApiController._

  val executionContexts: WithExecutionContexts
  val proxyResponseHandler: ProxyResponseHandler
  val authenticationMiddleware: AuthenticationMiddleware
  val authorizationMiddleware: AuthorizationMiddleware
  val makerCheckerMiddleware: MakerCheckerMiddleware
  val proxyRequestHandler: ProxyRequestHandler

  lazy val module: Module = Module(name = getRoute)
  lazy val scope: Scope = Scope(parent = getRoute)

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def composeProxyRequest[T](
    route: String,
    maybeCustomScope: Option[Scope] = None,
    maybeCustomModule: Option[Module] = None,
    maybeCustomContentType: Option[String] = None,
    requireAuthentication: Boolean = true,
    requireAuthorization: Boolean = true)(implicit ctx: Request[T]): Future[Result] = {

    implicit val requestId: UUID = getRequestId
    implicit val actualScope: Scope = maybeCustomScope.getOrElse(scope)
    implicit val actualModule: Module = maybeCustomModule.getOrElse(module)
    (for {
      backOfficeUser ← if (requireAuthentication) EitherT(authenticationMiddleware.getBackOfficeUser).map(u ⇒ Some(u)) else EitherT.fromEither[Future](None.asRight[ApiError])
      _ ← EitherT.fromEither[Future](if (requireAuthorization && backOfficeUser.isDefined)
        authorizationMiddleware.checkPermissions(backOfficeUser.get, ctx.method)
      else if (requireAuthorization && !backOfficeUser.isDefined)
        Left(ApiError(requestId, ApiErrorCodes.Forbidden, "User not found"))
      else Right(()))
      generalProxyRequest ← EitherT.fromEither[Future](proxyRequestHandler.createRequest(backOfficeUser, route).toEither)
        .leftMap(e ⇒ {
          logger.error(s"[composeProxyRequest] could not create proxy request, reason: ${e.getMessage}", e)
          s"could not create proxy request".asInvalidRequestApiError
        })

      proxyRequest ← EitherT.fromEither[Future] {
        if (makerCheckerMiddleware.isValidMakerCheckerRequest) makerCheckerMiddleware.createMakerCheckerRequest(generalProxyRequest).toEither
        else Right(generalProxyRequest)
      }.leftMap(e ⇒ {
        logger.error(s"[composeProxyRequest] could not create maker checker request, reason: ${e.getMessage}", e)
        s"could not create maker checker request".asInvalidRequestApiError
      })

      response ← EitherT(proxyResponseHandler.handleApiProxyResponse(proxyRequest, contentType = maybeCustomContentType.getOrElse("application/json")))

    } yield response).value.map(_.fold(makeApiErrorResponse, identity))
  }

}
