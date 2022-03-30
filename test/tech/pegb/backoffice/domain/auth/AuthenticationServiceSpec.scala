package tech.pegb.backoffice.domain.auth

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.auth.abstraction.BackOfficeUserDao
import tech.pegb.backoffice.dao.auth.dto.BackOfficeUserToUpdate
import tech.pegb.backoffice.dao.auth.entity.BackOfficeUser
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.auth.abstraction.{AuthenticationService, PasswordService, PermissionManagement, TokenService}
import tech.pegb.backoffice.domain.auth.dto.{BackOfficeUserCriteria, PermissionCriteria, TokenExpiration}
import tech.pegb.backoffice.domain.auth.model.{ClaimContent, Email, LoginStatusResponse, PasswordResetClaim}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.LoginUsername
import tech.pegb.backoffice.domain.{EmailClient, HttpClient, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class AuthenticationServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val notificationService = stub[EmailClient]
  private val backOfficeUserDao = stub[BackOfficeUserDao]
  private val permissionService = stub[PermissionManagement]
  private val httpClient = stub[HttpClient]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[BackOfficeUserDao].to(backOfficeUserDao),
      bind[PermissionManagement].to(permissionService),
      bind[EmailClient].to(notificationService),
      bind[HttpClient].to(httpClient),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val appConfig = inject[AppConfig]
  val authenticationService: AuthenticationService = inject[AuthenticationService]
  val passwordService: PasswordService = inject[PasswordService]
  val tokenService: TokenService = inject[TokenService]

  val baseRecaptcahUri = s"${appConfig.Authentication.recaptchaUrl}?secret=${appConfig.Authentication.recaptchaSecret}"
  implicit val tokenExpirationInMinutes = TokenExpiration(30)

  "AuthenticationService" should {

    val bUser1 = BackOfficeUser(
      id = UUID.randomUUID().toString,
      userName = "pumkinfreak",
      password = "pAssw0rd123".some,
      email = "d.salgado@pegb.tech",
      phoneNumber = "971529999".some,
      firstName = "David",
      middleName = none,
      lastName = "Salgado",
      description = none,
      homePage = none,
      activeLanguage = none,
      customData = none,
      lastLoginTimestamp = now.toZonedDateTimeUTC.toInstant.toEpochMilli.some,
      roleId = UUID.randomUUID().toString,
      roleName = "Manager",
      roleLevel = 1,
      roleCreatedBy = Some("pegbuser"),
      roleUpdatedBy = "pegbuser".some,
      roleCreatedAt = Some(LocalDateTime.now()),
      roleUpdatedAt = LocalDateTime.now().some,
      businessUnitId = UUID.randomUUID().toString,
      businessUnitName = "Finance",
      businessUnitCreatedBy = Some("pegbuser"),
      businessUnitUpdatedBy = "pegbuser".some,
      businessUnitCreatedAt = Some(LocalDateTime.now()),
      businessUnitUpdatedAt = LocalDateTime.now().some,
      isActive = 1,
      createdBy = Some("pegbuser"),
      updatedBy = "pegbuser".some,
      createdAt = Some(now),
      updatedAt = now.some)

    "login with username and password" in {

      val captcha = "someCaptcha"
      val newPassword = "NewP@ssword123!"
      val hashed = passwordService.hashPassword("NewP@ssword123!")

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some,
          hashedPassword = hashed.some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      (httpClient.requestWithRedirect _)
        .when(baseRecaptcahUri, Seq(("response", captcha)))
        .returns(Future.successful(HttpResponse(true, 301, None)))

      (backOfficeUserDao.updateLastLoginTimestamp _)
        .when(bUser1.id)
        .returns(bUser1.some.asRight[DaoError])

      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(bUser1.businessUnitId).some,
            roleId = UUIDLike(bUser1.roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))

      val futureResult = authenticationService.login(LoginUsername("pumkinfreak"), newPassword, captcha.some)

      val expected = bUser1.asDomain.get
      val claim = ClaimContent.from(expected)

      whenReady(futureResult) { result ⇒
        result.right.get._1 mustBe expected
        authenticationService.userClaim(result.right.get._2) mustBe claim.asRight[ServiceError]
      }
    }

    "lock account when login failed 3x" in {
      val captcha = "someCaptcha"

      //attempt1
      val incorrect1 = "wrongPass1!"
      val hashed1 = passwordService.hashPassword(incorrect1)
      (httpClient.requestWithRedirect _)
        .when(baseRecaptcahUri, Seq(("response", captcha)))
        .returns(Future.successful(HttpResponse(true, 301, None)))
      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some,
          hashedPassword = hashed1.some).asDao(true.some).some, None, None, None)
        .returns(Seq().asRight[DaoError])

      val futureResult1 = authenticationService.login(LoginUsername("pumkinfreak"), incorrect1, captcha.some)
      val expected1 = Left(ServiceError.notAuthorizedError(s"Wrong credentials for user pumkinfreak"))
      whenReady(futureResult1) { result ⇒
        result mustBe expected1
      }

      //attempt2
      val incorrect2 = "wrongPass2!"
      val hashed2 = passwordService.hashPassword(incorrect2)
      (httpClient.requestWithRedirect _)
        .when(baseRecaptcahUri, Seq(("response", captcha)))
        .returns(Future.successful(HttpResponse(true, 301, None)))
      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some,
          hashedPassword = hashed2.some).asDao(true.some).some, None, None, None)
        .returns(Seq().asRight[DaoError])

      val futureResult2 = authenticationService.login(LoginUsername("pumkinfreak"), incorrect2, captcha.some)
      val expected2 = Left(ServiceError.notAuthorizedError(s"Wrong credentials for user pumkinfreak"))
      whenReady(futureResult2) { result ⇒
        result mustBe expected2
      }

      //attempt3
      val incorrect3 = "wrongPass3!"
      val hashed3 = passwordService.hashPassword(incorrect3)
      (httpClient.requestWithRedirect _)
        .when(baseRecaptcahUri, Seq(("response", captcha)))
        .returns(Future.successful(HttpResponse(true, 301, None)))
      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some,
          hashedPassword = hashed3.some).asDao(true.some).some, None, None, None)
        .returns(Seq().asRight[DaoError])

      val futureResult3 = authenticationService.login(LoginUsername("pumkinfreak"), incorrect3, captcha.some)
      val expected3 = Left(ServiceError.accountLockedError(s"You've exceeded max login attempts (3). Your account has been locked for ${30} minutes."))
      whenReady(futureResult3) { result ⇒
        result mustBe expected3
      }

      //attempt4
      val incorrect4 = "wrongPass4!"
      val hashed4 = passwordService.hashPassword(incorrect4)
      (httpClient.requestWithRedirect _)
        .when(baseRecaptcahUri, Seq(("response", captcha)))
        .returns(Future.successful(HttpResponse(true, 301, None)))

      val futureResult4 = authenticationService.login(LoginUsername("pumkinfreak"), incorrect4, captcha.some)
      val expected4 = Left(ServiceError.accountLockedError(s"You've exceeded max login attempts (3). Your account has been locked for ${30} minutes."))
      whenReady(futureResult4) { result ⇒
        result mustBe expected4
      }

    }

    "update password" in {

      val oldPassword = "pAssw0rd123!"
      val oldHashed = passwordService.hashPassword(oldPassword)
      val newPassword = "NewP@ssword123!"
      val newHashed = passwordService.hashPassword(newPassword)

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some,
          hashedPassword = oldHashed.some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      (backOfficeUserDao.updateBackOfficeUser _)
        .when(
          bUser1.id,
          BackOfficeUserToUpdate(
            password = newHashed.some,
            lastUpdatedAt = bUser1.updatedAt,
            updatedBy = "pumkinfreak",
            updatedAt = now))
        .returns(bUser1.some.asRight[DaoError])

      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(bUser1.businessUnitId).some,
            roleId = UUIDLike(bUser1.roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))

      val futureResult = authenticationService.updatePassword(LoginUsername("pumkinfreak"), oldPassword, newPassword, now)

      val expected = bUser1.asDomain.get
      val claim = ClaimContent.from(expected)

      whenReady(futureResult) { result ⇒
        result.right.get._1 mustBe expected
        authenticationService.userClaim(result.right.get._2) mustBe claim.asRight[ServiceError]
      }
    }

    "fail to update password on empty string" in {

      val futureResult = authenticationService.updatePassword(LoginUsername("username"), "old_password", "", now)

      val expected = Left(ServiceError.validationError("Password cannot be null or empty"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "fail to update password, invalid password format" in {

      val futureResult = authenticationService.updatePassword(LoginUsername("username"), "old_password", "a", now)

      val expected = Left(ServiceError.validationError(s"Minimum password length not satisfied, Not enough uppercase characters in the password, Not enough special characters in the password, Not enough numeric characters in the password"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "return error when captcha is missing in sendPasswordResetLink" in {

      val email = Email("u.tyagi@pegb.tech")

      val futureResult = authenticationService.sendPasswordResetLink(LoginUsername("username"), email, None, None)

      val expected = Left(ServiceError.captchaRequiredError("`captcha` field is missing"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "send password link with referer and captcha" in {

      val email = Email("d.salgado@pegb.tech")
      val captcha = "captcha"
      val referer = "http://whizmo.app"

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      (httpClient.requestWithRedirect _)
        .when(baseRecaptcahUri, Seq(("response", captcha)))
        .returns(Future.successful(HttpResponse(true, 301, None)))

      (notificationService.sendEmail _)
        .when(Seq("d.salgado@pegb.tech"), "Reset password request", *)
        .returns(Right(()))

      val futureResult = authenticationService.sendPasswordResetLink(LoginUsername("pumkinfreak"), email, referer.some, captcha.some)

      val expected = Right(())

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "validate token" in {
      val claim = ClaimContent.from(bUser1.asDomain.get)

      val token = tokenService.generateToken("pumkinfreak", claim)

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(bUser1.businessUnitId).some,
            roleId = UUIDLike(bUser1.roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))

      val futureResult = authenticationService.validateToken(token)
      val expected = bUser1.asDomain.get

      whenReady(futureResult) { result ⇒
        result.isRight mustBe true
        val res = result.right.get
        res._1 mustBe expected
        authenticationService.userClaim(res._2) mustBe Right(claim)
      }
    }

    "fail validate token" in {

      val invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
      val futureResult = authenticationService.validateToken(invalidToken)

      val expected = Left(ServiceError.notAuthorizedError("User is not authorized"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "validate password reset token" in {
      val hashedPassword = "pAssw0rd123"
      val passwordResetClaim = PasswordResetClaim("pumkinfreak", hashedPassword.some)

      val token = tokenService.generateToken("pumkinfreak", passwordResetClaim)

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      val futureResult = authenticationService.validatePasswordResetToken(token)

      val expected = Right(())

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "fail to validate password reset token" in {

      val invalidToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJQRUdCIiwiYXVkIjpbInB1bWtpbmZyZWFrIl0sImV4cCI6MTU3MzM4MDQ1NCwiaWF0IjoxNTczMzc4NjU0LCJ1c2VyTmFtZSI6Imwud2FyZEBnbWFpbC5jb20ifQ.Xo3TUb2WsdAhB7dphWoTRTVOIWMOk9fhjftrNtlnpb6aqDWv_QxtIOF0J_DVZSW6r9uXapazOL2yCM9a5wjQdg"
      val futureResult = authenticationService.validatePasswordResetToken(invalidToken)

      val expected = Left(ServiceError.notAuthorizedError("User is not authorized"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "fail to validate password reset token when it's used alread (password is different)" in {
      val hashedPassword = "oldHashedPassword"
      val passwordResetClaim = PasswordResetClaim("pumkinfreak", hashedPassword.some)

      val invalidToken = tokenService.generateToken("pumkinfreak", passwordResetClaim)

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])
      val futureResult = authenticationService.validatePasswordResetToken(invalidToken)

      val expected = Left(ServiceError.notAuthorizedError("Token was already used to reset password"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "reset password" in {
      val hashedPassword = "pAssw0rd123"
      val passwordResetClaim = PasswordResetClaim("pumkinfreak", hashedPassword.some)
      val token = tokenService.generateToken("pumkinfreak", passwordResetClaim)
      val newPassword = "NewP@ssword123!"
      val hashed = passwordService.hashPassword("NewP@ssword123!")

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      (backOfficeUserDao.updateBackOfficeUser _)
        .when(
          bUser1.id,
          BackOfficeUserToUpdate(
            password = hashed.some,
            lastUpdatedAt = bUser1.updatedAt,
            updatedBy = "pumkinfreak",
            updatedAt = now))
        .returns(bUser1.some.asRight[DaoError])

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(
          BackOfficeUserCriteria(
            userName = "pumkinfreak".some, hashedPassword = hashed.some).asDao(true.some).some,
          None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      (backOfficeUserDao.updateLastLoginTimestamp _)
        .when(bUser1.id)
        .returns(bUser1.some.asRight[DaoError])

      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(bUser1.businessUnitId).some,
            roleId = UUIDLike(bUser1.roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))

      val futureResult = authenticationService.resetPassword(newPassword, token, now)

      val expected = bUser1.asDomain.get
      val claim = ClaimContent.from(expected)

      whenReady(futureResult) { result ⇒
        result.map(_._1) mustBe Right(expected)
        authenticationService.userClaim(result.right.get._2) mustBe Right(claim)
      }
    }

    "fail to reset password on empty token" in {

      val futureResult = authenticationService.resetPassword("reset_password", "", now)

      val expected = Left(ServiceError.notAuthorizedError("User is not authorized"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "fail to reset password on empty password" in {
      val hashedPassword = "pAssw0rd123"
      val passwordResetClaim = PasswordResetClaim("pumkinfreak", hashedPassword.some)
      val token = tokenService.generateToken("pumkinfreak", passwordResetClaim)
      val newPassword = ""

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      val futureResult = authenticationService.resetPassword(newPassword, token, now)

      val expected = Left(ServiceError.validationError("Password cannot be null or empty"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "fail to reset password when token has been used alread (token hashed password is different from in db)" in {
      val hashedPassword = "oldPAssw0rd123"
      val passwordResetClaim = PasswordResetClaim("pumkinfreak", hashedPassword.some)
      val token = tokenService.generateToken("pumkinfreak", passwordResetClaim)
      val newPassword = "NewP@ssword123!"
      val hashed = passwordService.hashPassword("NewP@ssword123!")

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1).asRight[DaoError])

      val futureResult = authenticationService.resetPassword(newPassword, token, now)

      val expected = Left(ServiceError.notAuthorizedError("Token was already used to reset password"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "fail to reset password when new password is same as in token" in {
      val hashedPassword = passwordService.hashPassword("pAssw0rd123!")
      val passwordResetClaim = PasswordResetClaim("pumkinfreak", hashedPassword.some)
      val token = tokenService.generateToken("pumkinfreak", passwordResetClaim)
      val newPassword = "pAssw0rd123!"

      (backOfficeUserDao.getBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(
          userName = "pumkinfreak".some).asDao(true.some).some, None, None, None)
        .returns(Seq(bUser1.copy(password = hashedPassword.some)).asRight[DaoError])

      val futureResult = authenticationService.resetPassword(newPassword, token, now)

      val expected = Left(ServiceError.notAuthorizedError("New password cannot be the same as old password"))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }

    "get status" in {

      val futureResult = authenticationService.status

      val expected = Right(LoginStatusResponse(true))

      whenReady(futureResult) { result ⇒
        result mustBe expected
      }
    }
  }

  "TokenBehavior and AuthenticationBehavior" should {

    "encode and decode ClaimContent correctly " in {
      val claim = ClaimContent(
        loggedInAs = "d.salgado",
        email = Email("d.salgado@pegb.tech"))

      val token = tokenService.generateToken("d.salgado", claim)
      val decoded = authenticationService.userClaim(token)

      decoded mustBe Right(claim)
    }

    "encode and decode PasswordResetClaim correctly " in {
      val hashedPassword = passwordService.hashPassword("pAssw0rd123")
      val passwordResetClaim = PasswordResetClaim("l.ward@gmail.com", hashedPassword.some)

      val token = tokenService.generateToken("pumkinfreak", passwordResetClaim)
      val decoded = authenticationService.passwordResetClaim(token)

      decoded mustBe Right(passwordResetClaim)
    }
  }
}
