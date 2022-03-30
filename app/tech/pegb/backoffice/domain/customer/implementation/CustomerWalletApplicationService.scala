package tech.pegb.backoffice.domain.customer.implementation

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.Inject
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.application.model.WalletApplication
import tech.pegb.backoffice.domain.customer.abstraction.CustomerWalletApplication
import tech.pegb.backoffice.domain.customer.error.{InactiveCustomerFound}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future

class CustomerWalletApplicationService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    val walletApplicationManagement: WalletApplicationManagement) extends CustomerWalletApplication {

  private val activeUserStatus = conf.ActiveUserStatus
  implicit val executionContext = executionContexts.blockingIoOperations

  def getWalletApplicationsByUserId(userUuid: UUID): Future[ServiceResponse[Set[WalletApplication]]] = {
    val result = for {
      maybeIsUserActive ← userDao.getUser(userUuid.toString).asServiceResponse
      user ← maybeIsUserActive.toRight(notFoundError(s"user with uuid $userUuid not found"))

      isUserActive ← user.status.map(_.contains(activeUserStatus))
        .toRight(notFoundError(s"no user status found for user $user"))
    } yield {
      if (isUserActive)
        walletApplicationManagement.getWalletApplicationByUserUuid(userUuid)
      else
        Future.successful(Left(InactiveCustomerFound(user)))
    }

    result match {
      case Left(value) ⇒ Future.successful(Left(value))
      case Right(rightResult) ⇒ rightResult
    }

  }

  def getWalletApplicationByApplicationIdAndUserId(userUuid: UUID, applicationUuid: UUID): Future[ServiceResponse[WalletApplication]] = {
    val result = for {
      maybeIsUserActive ← userDao.getUser(userUuid.toString).asServiceResponse
      user ← maybeIsUserActive.toRight(notFoundError(s"user with uuid $userUuid not found"))

      isUserActive ← user.status.map(_.contains(activeUserStatus))
        .toRight(notFoundError(s"no user status found for user $user"))

    } yield {
      if (isUserActive)
        walletApplicationManagement.getWalletApplicationById(applicationUuid)
      else
        Future.successful(Left(InactiveCustomerFound(user)))
    }

    result match {
      case Left(value) ⇒ Future.successful(Left(value))
      case Right(rightResult) ⇒ rightResult
    }
  }

  def approveWalletApplicationByUserId(userUuid: UUID, applicationUuid: UUID,
    approvedBy: String, approvedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]] = {

    val result = for {
      maybeIsUserActive ← userDao.getUser(userUuid.toString).asServiceResponse
      user ← maybeIsUserActive.toRight(notFoundError(s"user with uuid $userUuid not found"))

    } yield {
      walletApplicationManagement.approvePendingWalletApplication(applicationUuid, approvedBy, approvedAt, lastUpdatedAt)
    }

    result match {
      case Left(value) ⇒ Future.successful(Left(value))
      case Right(rightResult) ⇒ rightResult
    }
  }

  def rejectWalletApplicationByUserId(userUuid: UUID, applicationUUID: UUID, rejectedBy: String, rejectedAt: LocalDateTime, reason: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]] = {

    val result = for {
      maybeIsUserActive ← userDao.getUser(userUuid.toString).asServiceResponse
      user ← maybeIsUserActive.toRight(notFoundError(s"user with uuid $userUuid not found"))

    } yield {
      walletApplicationManagement.rejectPendingWalletApplication(applicationUUID, rejectedBy, rejectedAt, reason, lastUpdatedAt)
    }

    result match {
      case Left(value) ⇒ Future.successful(Left(value))
      case Right(rightResult) ⇒ rightResult
    }
  }
}
