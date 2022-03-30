package tech.pegb.backoffice.domain.auth.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.auth.model._

import scala.concurrent.Future
import tech.pegb.backoffice.domain.auth._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.LoginUsername

@ImplementedBy(classOf[implementation.AuthenticationServiceImpl])
trait AuthenticationService {

  def login(
    userName: LoginUsername,
    password: String,
    maybeCaptcha: Option[String]): Future[ServiceResponse[(BackOfficeUser, String)]]

  def updatePassword(
    userName: LoginUsername,
    oldPassword: String,
    newPassword: String,
    updatedAt: LocalDateTime): Future[ServiceResponse[(BackOfficeUser, String)]]

  def sendPasswordResetLink(
    userName: LoginUsername,
    email: Email,
    maybeReferer: Option[String],
    maybeCaptcha: Option[String]): Future[ServiceResponse[Unit]]

  def validatePasswordResetToken(token: String): Future[ServiceResponse[Unit]]

  def resetPassword(password: String, token: String, resetTimestamp: LocalDateTime): Future[ServiceResponse[(BackOfficeUser, String)]]

  def status: Future[ServiceResponse[LoginStatusResponse]]

  def validateToken(token: String): Future[ServiceResponse[(BackOfficeUser, String)]]

  def userClaim(rawToken: String): ServiceResponse[ClaimContent]

  def passwordResetClaim(rawToken: String): ServiceResponse[PasswordResetClaim]

}
