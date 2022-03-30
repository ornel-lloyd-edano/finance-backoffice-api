package tech.pegb.backoffice.domain.auth.implementation

import java.time.LocalDateTime

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.libs.json.Json
import scalacache._
import scalacache.caffeine._
import scalacache.modes.scalaFuture._
import tech.pegb.backoffice.dao.auth.abstraction.BackOfficeUserDao
import tech.pegb.backoffice.dao.auth.dto.BackOfficeUserToUpdate
import tech.pegb.backoffice.domain.auth._
import tech.pegb.backoffice.domain.auth.abstraction.{PasswordService, PermissionManagement, TokenService}
import tech.pegb.backoffice.domain.auth.dto.{BackOfficeUserCriteria, PermissionCriteria, TokenExpiration}
import tech.pegb.backoffice.domain.auth.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.LoginUsername
import tech.pegb.backoffice.domain.{BaseService, EmailClient, ErrorCodes, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

class AuthenticationServiceImpl @Inject() (
    appConfig: AppConfig,
    backOfficeUserDao: BackOfficeUserDao,
    permissionManagement: PermissionManagement,
    passwordService: PasswordService,
    tokenService: TokenService,
    captchaService: CaptchaService,
    notificationService: EmailClient,
    executionContexts: WithExecutionContexts) extends abstraction.AuthenticationService
  with BaseService {

  import AuthenticationServiceImpl._
  import ClaimContent._
  import PasswordResetClaim._

  private val authConfig = appConfig.Authentication
  private val lockDuration = authConfig.accountLockTimeout
  private val maxBadAttempts = authConfig.maxBadLoginAttempts
  private val captchaRequired: Boolean = authConfig.requireCaptcha
  private val appHost = appConfig.BackOfficeHost
  private implicit val loginCache: CaffeineCache[Long] = CaffeineCache[Long](CacheConfig.defaultCacheConfig)
  implicit val ec = executionContexts.blockingIoOperations

  implicit val tokenExpirationInMinutes = TokenExpiration(appConfig.Authentication.tokenExpirationOffsetMinutes)

  def login(
    userName: LoginUsername,
    password: String,
    maybeCaptcha: Option[String]): Future[ServiceResponse[(BackOfficeUser, String)]] = {
    val hashedPassword = passwordService.hashPassword(password)
    (for {
      loginAttempts ← EitherT(incUserLoginTriesCache(userName.underlying))
      _ ← EitherT(checkCaptcha(maybeCaptcha))
      user ← EitherT(loginHelper(userName.underlying, hashedPassword))
        .leftMap { err ⇒
          if (loginAttempts == maxBadAttempts && err.code != ErrorCodes.AccountTemporarilyLocked) {
            buildLockErrorMessage
          } else {
            err
          }
        }
      _ ← EitherT.liftF[Future, ServiceError, Any](cleanUserLoginCache(user.userName))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = tokenService.generateToken(userName.underlying, claimContent)
      user → token
    }).value
  }

  def updatePassword(
    userName: LoginUsername,
    oldPassword: String,
    newPassword: String,
    updatedAt: LocalDateTime): Future[ServiceResponse[(BackOfficeUser, String)]] = {
    (for {
      hashes ← EitherT.fromEither[Future](passwordService.validatePassword(Some(oldPassword), newPassword))
        .map(_ ⇒ passwordService.hashPassword(oldPassword) → passwordService.hashPassword(newPassword))
      (oldPasswordHash, newPasswordHash) = hashes
      user ← EitherT(updatePasswordHelper(userName.underlying, oldPasswordHash, newPasswordHash, updatedAt))
      _ ← EitherT.liftF[Future, ServiceError, Any](cleanUserLoginCache(user.userName))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = tokenService.generateToken(userName.underlying, claimContent)
      user → token
    }).value
  }

  def sendPasswordResetLink(
    userName: LoginUsername,
    email: Email,
    maybeReferer: Option[String],
    maybeCaptcha: Option[String]): Future[ServiceResponse[Unit]] = {
    (for {
      _ ← EitherT(checkCaptcha(maybeCaptcha))
      getBackOfficeResultOption ← EitherT.fromEither[Future](backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria = BackOfficeUserCriteria(userName = userName.underlying.some).asDao(true.some).some,
        orderBy = None,
        limit = None,
        offset = None).asServiceResponse)
      user ← EitherT.fromOption[Future](getBackOfficeResultOption.headOption, notFoundError(s"${userName.underlying} not found"))
      notification ← EitherT.fromEither[Future](
        if (user.email == email.value) {
          val resetPasswordToken = tokenService.generateToken(userName.underlying, PasswordResetClaim(userName.underlying, user.password))

          val resetPasswordLink = s"${maybeReferer.getOrElse(appHost)}/reset_password?token=$resetPasswordToken"

          val resetPasswordMessage =
            s"""Hello ${userName.underlying},
               |
               |Click the following link to reset your password: $resetPasswordLink.
               |The link will expire in $tokenExpirationInMinutes minutes.""".stripMargin

          notificationService.sendEmail(Seq(user.email), "Reset password request", resetPasswordMessage)
            .leftMap(t ⇒ {
              logger.error("Error in [sendPasswordResetLink]", t)
              unknownError(s"Failed to send email notification to ${user.email}")
            })
        } else {
          Left(notFoundError(s"There is no such user as $userName/$email"))
        })
    } yield notification).value
  }

  def validatePasswordResetToken(token: String): Future[ServiceResponse[Unit]] = {
    (for {
      pwordResetClaim ← EitherT.fromEither[Future](passwordResetClaim(token))
      getBackOfficeResultList ← EitherT.fromEither[Future](backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria = BackOfficeUserCriteria(userName = pwordResetClaim.userName.some).asDao(true.some).some,
        orderBy = None,
        limit = None,
        offset = None).asServiceResponse)
      backOfficeUser ← EitherT.fromOption[Future](getBackOfficeResultList.headOption, notAuthorizedError(s"Invalid Token"))
      _ ← EitherT.cond[Future](pwordResetClaim.hashedPassword == backOfficeUser.password, (), notAuthorizedError(s"Token was already used to reset password"))
    } yield {
      ()
    }).value
  }

  def resetPassword(password: String, token: String, resetTimestamp: LocalDateTime): Future[ServiceResponse[(BackOfficeUser, String)]] = {
    (for {
      _ ← EitherT(validatePasswordResetToken(token))
      resetClaim ← EitherT.fromEither[Future](passwordResetClaim(token))
      passwordHash ← EitherT.fromEither[Future](passwordService.validatePassword(None, password))
        .map(passwordService.hashPassword)
      _ ← EitherT.cond[Future](!resetClaim.hashedPassword.contains(passwordHash), (), validationError("New password cannot be the same as old password"))
      user ← EitherT(resetPasswordHelper(resetClaim.userName, passwordHash, resetTimestamp))
      _ ← EitherT.liftF[Future, ServiceError, Any](cleanUserLoginCache(user.userName))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = tokenService.generateToken(resetClaim.userName, claimContent)
      user → token
    }).value
  }

  def status: Future[ServiceResponse[LoginStatusResponse]] = Future {
    Right(LoginStatusResponse(requireCaptcha = captchaRequired))
  }

  def validateToken(token: String): Future[ServiceResponse[(BackOfficeUser, String)]] = {
    (for {
      claim ← EitherT.fromEither[Future](userClaim(token))
      getBackOfficeResultOption ← EitherT.fromEither[Future](backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria = BackOfficeUserCriteria(userName = claim.loggedInAs.some).asDao(true.some).some,
        orderBy = None,
        limit = None,
        offset = None).asServiceResponse)
      userDao ← EitherT.fromOption[Future](getBackOfficeResultOption.headOption, notFoundError(s"${claim.loggedInAs} not found"))
      user ← EitherT.fromEither[Future](userDao.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert backoffice entity to domain: ${t.getMessage}")))
      permissions ← EitherT(permissionManagement.getPermissionByCriteria(
        PermissionCriteria(
          businessId = UUIDLike(user.businessUnit.id.toString).some,
          roleId = UUIDLike(user.role.id.toString).some),
        Nil, None, None))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = tokenService.generateToken(user.userName, claimContent)
      (user.copy(permissions = permissions) → token)
    }).value
  }

  /* Authentication Behavior functions */

  private def decodeClaim(rawToken: String): Try[JwtClaim] = {
    JwtJson.decode(rawToken, publicKey, algorithms)
  }

  def userClaim(rawToken: String): ServiceResponse[ClaimContent] = {
    for {
      // .decode takes care about expiration and signature validation
      claim ← decodeClaim(rawToken).toEither
        .leftMap(e ⇒ {
          logger.error("error encountered in [userClaim]", e)
          notAuthorizedError("User is not authorized")
        })
      userClaim ← claimContentFormat.reads(Json.parse(claim.content)).asEither
        .leftMap(e ⇒ {
          logger.error("error encountered in [userClaim]", e)
          notAuthorizedError("User is not authorized")
        })
    } yield userClaim
  }

  def passwordResetClaim(rawToken: String): ServiceResponse[PasswordResetClaim] = {
    for {
      // .decode takes care about expiration and signature validation
      claim ← decodeClaim(rawToken).toEither
        .leftMap(e ⇒ {
          logger.error("error encountered in [passwordResetClaim]", e)
          notAuthorizedError("User is not authorized")
        })
      passwordResetClaim ← passwordResetClaimFormat.reads(Json.parse(claim.content)).asEither
        .leftMap(e ⇒ {
          logger.error("error encountered in [passwordResetClaim]", e)
          notAuthorizedError("User is not authorized")
        })
    } yield {
      passwordResetClaim
    }
  }

  /* helper functions */

  protected def loginHelper(userName: String, hashedPassword: String): Future[ServiceResponse[BackOfficeUser]] = {
    (for {
      getBackOfficeResultList ← EitherT.fromEither[Future](backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria = BackOfficeUserCriteria(userName = userName.some, hashedPassword = hashedPassword.some).asDao(true.some).some,
        orderBy = None,
        limit = None,
        offset = None).asServiceResponse)
      getBackOfficeResult ← EitherT.fromOption[Future](getBackOfficeResultList.headOption, notAuthorizedError(s"Wrong credentials for user $userName"))
      updatedBackOfficeUserOption ← EitherT.fromEither[Future](backOfficeUserDao.updateLastLoginTimestamp(id = getBackOfficeResult.id).asServiceResponse)
      updatedBackOfficeUser ← EitherT.fromOption[Future](updatedBackOfficeUserOption, notAuthorizedError(s"Wrong credentials for user $userName"))
      domainBackOfficeUser ← EitherT.fromEither[Future](updatedBackOfficeUser.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert backoffice entity to domain: ${t.getMessage}")))
      permissions ← EitherT(permissionManagement.getPermissionByCriteria(
        PermissionCriteria(
          businessId = UUIDLike(domainBackOfficeUser.businessUnit.id.toString).some,
          roleId = UUIDLike(domainBackOfficeUser.role.id.toString).some),
        Nil, None, None))
    } yield {
      domainBackOfficeUser.copy(permissions = permissions)
    }).value
  }

  protected def updatePasswordHelper(
    userName: String,
    oldPasswordHash: String,
    passwordHash: String,
    loginTime: LocalDateTime): Future[ServiceResponse[BackOfficeUser]] = {

    (for {
      getBackOfficeResultOption ← EitherT.fromEither[Future](backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria = BackOfficeUserCriteria(userName = userName.some, hashedPassword = oldPasswordHash.some).asDao(true.some).some,
        orderBy = None,
        limit = None,
        offset = None).asServiceResponse)
      getBackOfficeResult ← EitherT.fromOption[Future](getBackOfficeResultOption.headOption, notAuthorizedError(s"Failed to update password for user $userName. Check credentials"))
      updatedBackOfficeUserOption ← EitherT.fromEither[Future](backOfficeUserDao.updateBackOfficeUser(
        id = getBackOfficeResult.id,
        dto = BackOfficeUserToUpdate(
          password = passwordHash.some,
          lastUpdatedAt = getBackOfficeResult.updatedAt,
          updatedBy = userName,
          updatedAt = loginTime)).asServiceResponse)
      updatedBackOfficeUser ← EitherT.fromOption[Future](updatedBackOfficeUserOption, notAuthorizedError(s"Failed to update password for user $userName. Check credentials"))
      domainBackOfficeUser ← EitherT.fromEither[Future](updatedBackOfficeUser.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert backoffice entity to domain: ${t.getMessage}")))
      permissions ← EitherT(permissionManagement.getPermissionByCriteria(
        PermissionCriteria(
          businessId = UUIDLike(domainBackOfficeUser.businessUnit.id.toString).some,
          roleId = UUIDLike(domainBackOfficeUser.role.id.toString).some),
        Nil, None, None))
    } yield {
      domainBackOfficeUser.copy(permissions = permissions)
    }).value
  }

  protected def resetPasswordHelper(userName: String, hashedPassword: String, resetTime: LocalDateTime): Future[ServiceResponse[BackOfficeUser]] = {
    (for {
      getBackOfficeResultOption ← EitherT.fromEither[Future](backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria = BackOfficeUserCriteria(userName = userName.some).asDao(true.some).some,
        orderBy = None,
        limit = None,
        offset = None).asServiceResponse)
      getBackOfficeResult ← EitherT.fromOption[Future](getBackOfficeResultOption.headOption, notFoundError(s"$userName not found"))
      updatedBackOfficeUser ← EitherT.fromEither[Future](backOfficeUserDao.updateBackOfficeUser(
        id = getBackOfficeResult.id,
        dto = BackOfficeUserToUpdate(
          password = hashedPassword.some,
          lastUpdatedAt = getBackOfficeResult.updatedAt,
          updatedBy = userName,
          updatedAt = resetTime)).asServiceResponse)
      loginResult ← EitherT(loginHelper(userName, hashedPassword))
    } yield {
      loginResult
    }).value
  }

  protected def checkCaptcha(maybeCaptcha: Option[String]): Future[ServiceResponse[Unit]] = {
    if (captchaRequired) {
      maybeCaptcha.fold[Future[ServiceResponse[Unit]]] {
        Future.successful(Left(ServiceError.captchaRequiredError("`captcha` field is missing")))
      } {
        captchaService.checkCaptcha
      }
    } else {
      Future.successful(Right(()))
    }
  }

  protected def incUserLoginTriesCache(userName: String): Future[ServiceResponse[Long]] = {
    val cacheKey = makeLoginCacheKey(userName)
    for {
      maybeCachedTries ← get[Future, Long](cacheKey)
      tries = {
        logCacheHitOrMiss(cacheKey, maybeCachedTries)
        maybeCachedTries.getOrElse(0L) + 1
      }
      okOrError ← if (tries > maxBadAttempts) {
        Future.successful(Left(buildLockErrorMessage))
      } else {
        logCachePut(key = cacheKey, Some(lockDuration))
        put(cacheKey)(value = tries, ttl = Some(lockDuration))
          .map(_ ⇒ Right(tries))
      }
    } yield okOrError
  }

  protected def cleanUserLoginCache(userName: String): Future[Any] = {
    remove(makeLoginCacheKey(userName))
  }

  @inline private def makeLoginCacheKey(userName: String): String = userName + CacheLoginTries

  protected def buildLockErrorMessage: ServiceError = {
    val msg = s"You've exceeded max login attempts ($maxBadAttempts). " +
      s"Your account has been locked for ${lockDuration.toMinutes} minutes."
    ServiceError.accountLockedError(msg)
  }

  /**
   * Copied from scalacache.LoggingSupport as `logger` from it has type different from `logger` here
   * Output a debug log to record the result of a cache lookup
   *
   * @param key the key that was looked up
   * @param result the result of the cache lookup
   * @tparam A the type of the cache value
   */
  protected def logCacheHitOrMiss[A](key: String, result: Option[A]): Unit = {
    if (logger.isDebugEnabled) {
      val hitOrMiss = result.map(_ ⇒ "hit") getOrElse "miss"
      logger.debug(s"Cache $hitOrMiss for key $key")
    }
  }

  /**
   * Copied from scalacache.LoggingSupport as `logger` from it has type different from `logger` here
   * Output a debug log to record a cache insertion/update
   *
   * @param key the key that was inserted/updated
   * @param ttl the TTL of the inserted entry
   */
  protected def logCachePut(key: String, ttl: Option[Duration]): Unit = {
    if (logger.isDebugEnabled) {
      val ttlMsg = ttl.map(d ⇒ s" with TTL ${d.toMillis} ms") getOrElse ""
      logger.debug(s"Inserted value into cache with key $key$ttlMsg")
    }
  }

}

object AuthenticationServiceImpl {
  private final val CacheLoginTries = ".loginTries"
  private val publicKey: String = scala.io.Source.fromResource("auth.key.pub").mkString
  private val algorithms: Seq[JwtAsymmetricAlgorithm] = Seq(JwtAlgorithm.ES256)
}
