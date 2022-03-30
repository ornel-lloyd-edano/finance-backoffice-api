package tech.pegb.backoffice.api.auth

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}

import play.api.mvc.Request
import tech.pegb.backoffice.api.{ApiError, RequiredHeaders}
import tech.pegb.backoffice.domain.auth.abstraction.{AuthenticationService, BackOfficeUserService}
import tech.pegb.backoffice.domain.auth.model.BackOfficeUser
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationMiddleware @Inject() (
    executionContexts: WithExecutionContexts,
    authenticationService: AuthenticationService,
    backOfficeUserService: BackOfficeUserService,
    implicit val appConfig: AppConfig) extends RequiredHeaders with Logging {

  implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  def getBackOfficeUser[T](implicit ctx: Request[T], requestId: UUID): Future[Either[ApiError, BackOfficeUser]] = {
    (for {
      extractedToken ← EitherT(getTokenFromRequest.toFuture)
      claimContent ← EitherT(authenticationService.userClaim(extractedToken).toFuture).leftMap(_.asApiError())
      backOfficeUser ← EitherT(backOfficeUserService.getBackOfficeUserByUsername(claimContent.loggedInAs)).leftMap(_.asApiError())

    } yield {
      logger.info("authenticated user from token: " + backOfficeUser.userName)
      logger.info(s"available permissions for ${backOfficeUser.userName}: " + backOfficeUser.permissions.map(_.scope.name).mkString("[", ", ", "]"))
      backOfficeUser
    }).value
  }

}
