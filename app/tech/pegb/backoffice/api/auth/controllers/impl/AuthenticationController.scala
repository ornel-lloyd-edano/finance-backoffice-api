package tech.pegb.backoffice.api.auth.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.auth._
import tech.pegb.backoffice.api.auth.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.SuccessfulStatuses
import tech.pegb.backoffice.domain.auth.abstraction.AuthenticationService
import tech.pegb.backoffice.mapping.api.domain.auth.authentication.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticationController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    authenticationService: AuthenticationService,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with controllers.AuthenticationController {

  import AuthenticationController._
  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def login: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      creds ← EitherT.fromEither[Future](
        ctx.body.toString().as(classOf[CredentialsToRead], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedLoginCredentialsErrorMsg.some)))

      username ← EitherT.fromEither[Future](creds.user.asLoginUsername().toEither
        .leftMap(t ⇒ t.log().asInvalidRequestApiError(t.getMessage.some)))

      password ← EitherT.cond[Future](
        creds.password.hasSomething,
        creds.password,
        ApiError(requestId, ApiErrorCodes.InvalidRequest, "Password cannot be null or empty"))

      loginResp ← EitherT(authenticationService.login(username, password, creds.captcha))
        .leftMap(_.asApiError())
    } yield {
      LoginResponse(
        token = loginResp._2,
        user = loginResp._1.asApi).toJsonStr
    }).value.map(handleApiResponse(_))

  }

  def updatePassword: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate

    (for {
      creds ← EitherT.fromEither[Future](
        ctx.body.toString().as(classOf[CredentialsToUpdate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateCredentialsErrorMsg.some)))

      username ← EitherT.fromEither[Future](creds.user.asLoginUsername().toEither
        .leftMap(t ⇒ t.log().asInvalidRequestApiError(t.getMessage.some)))

      oldPassword ← EitherT.cond[Future](
        creds.oldPassword.hasSomething,
        creds.oldPassword,
        ApiError(requestId, ApiErrorCodes.InvalidRequest, "Password cannot be null or empty"))

      loginResp ← EitherT(authenticationService.updatePassword(username, oldPassword, creds.newPassword, doneAt.toLocalDateTimeUTC))
        .leftMap(_.asApiError())
    } yield {
      LoginResponse(
        token = loginResp._2,
        user = loginResp._1.asApi).toJsonStr
    }).value.map(handleApiResponse(_))

  }

  def resetPassword: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      passwordResetRequest ← EitherT.fromEither[Future](
        ctx.body.toString().as(classOf[ResetPasswordLinkRequest], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateCredentialsErrorMsg.some)))

      username ← EitherT.fromEither[Future](passwordResetRequest.userName.asLoginUsername().toEither
        .leftMap(t ⇒ t.log().asInvalidRequestApiError(t.getMessage.some)))

      email ← EitherT.fromEither[Future](passwordResetRequest.email.asEmail().toEither
        .leftMap(t ⇒ t.log().asInvalidRequestApiError(t.getMessage.some)))

      resp ← EitherT {
        val maybeCaptcha = passwordResetRequest.captcha
        val maybeReferer = ctx.headers.get(REFERER)
        logger.info("Referer is " + maybeReferer.getOrElse("missing"))
        authenticationService.sendPasswordResetLink(username, email, None, maybeCaptcha) //TODO: make referer None
      }.leftMap(_.asApiError())

    } yield {
      JsObject.empty
    }).value.map(r ⇒ handleApiResponse(r, SuccessfulStatuses.Ok))

  }

  def validateResetPasswordByToken(token: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      resp ← EitherT(authenticationService.validatePasswordResetToken(token)).leftMap(_.asApiError())
    } yield {
      JsObject.empty
    }).value.map(r ⇒ handleApiResponse(r, SuccessfulStatuses.Ok))
  }

  def updateResetPassword: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate

    (for {
      pwdResetForm ← EitherT.fromEither[Future](
        ctx.body.toString().as(classOf[PasswordReset], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateCredentialsErrorMsg.some)))

      loginResp ← EitherT(authenticationService.resetPassword(pwdResetForm.password, pwdResetForm.token, doneAt.toLocalDateTimeUTC))
        .leftMap(_.asApiError())
    } yield {
      LoginResponse(
        token = loginResp._2,
        user = loginResp._1.asApi).toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def getStatus: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      resp ← EitherT(authenticationService.status).leftMap(_.asApiError())
    } yield {
      resp.toJsonStr
    }).value.map(handleApiResponse(_))

  }

  def validateToken: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      token ← EitherT.fromEither[Future](getTokenFromRequest)
      resp ← EitherT(authenticationService.validateToken(token)).leftMap(_.asApiError())
    } yield {
      LoginResponse(
        token = resp._2,
        user = resp._1.asApi).toJsonStr
    }).value.map(handleApiResponse(_))
  }
}

object AuthenticationController {
  val MalformedLoginCredentialsErrorMsg = "Malformed request to login. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateCredentialsErrorMsg = "Malformed request to update credentials. Mandatory field is missing or value of a field is of wrong type."

}
