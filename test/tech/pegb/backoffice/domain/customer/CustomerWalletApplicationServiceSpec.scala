package tech.pegb.backoffice.domain.customer

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.application.model.{ApplicationStatus, WalletApplication}
import tech.pegb.backoffice.domain.customer.error.{InactiveCustomerFound}
import tech.pegb.backoffice.domain.customer.implementation.CustomerWalletApplicationService
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class CustomerWalletApplicationServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with BaseService {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val userDao = stub[UserDao]
  val walletApplicationManagement = stub[WalletApplicationManagement]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[UserDao].to(userDao),
      bind[WalletApplicationManagement].to(walletApplicationManagement),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val customerApplicationWalletService = inject[CustomerWalletApplicationService]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val applicationUuidOne = UUID.randomUUID()
  val applicationUuidTwo = UUID.randomUUID()
  val customerUuidOne = UUID.randomUUID()
  val customerUuidTwo = UUID.randomUUID()

  val walletApplicationOne = WalletApplication.getEmpty.copy(
    id = applicationUuidOne,
    customerId = customerUuidOne,
    fullName = Some("Dima Test Linou"),
    msisdn = Some(Msisdn(underlying = "+971582181475")),
    status = ApplicationStatus(underlying = "pending"),
    applicationStage = "OCR",
    checkedBy = Some("ujali"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val walletApplicationTwo = WalletApplication.getEmpty.copy(
    id = applicationUuidTwo,
    customerId = customerUuidOne,
    fullName = Some("Dima Test Linou"),
    msisdn = Some(Msisdn(underlying = "+9715821821231")),
    status = ApplicationStatus(underlying = "approved"),
    applicationStage = "OCR",
    checkedBy = Some("test"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val user = User(
    id = 1,
    uuid = customerUuidOne.toString,
    userName = "iu_username",
    password = Option("iu_password"),
    `type` = Option(config.IndividualUserType),
    tier = None,
    segment = None,
    subscription = None,
    email = Option("iu@gmail.com"),
    status = Option(config.ActiveUserStatus),
    activatedAt = Option(LocalDateTime.now(mockClock)),
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "pegbuser",
    updatedAt = None,
    updatedBy = None)

  val userWithBusinessUserType = User(
    id = 2,
    uuid = customerUuidOne.toString,
    userName = "iu_username",
    password = Option("iu_password"),
    `type` = Option(config.BusinessUserType),
    tier = None,
    segment = None,
    subscription = None,
    email = Option("iu@gmail.com"),
    status = Option(config.ActivatedBusinessUserStatus),
    activatedAt = Option(LocalDateTime.now(mockClock)),
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "pegbuser",
    updatedAt = None,
    updatedBy = None)

  val individualUserInvalid = User(
    id = 3,
    uuid = customerUuidOne.toString,
    userName = "iu_username",
    password = Option("iu_password"),
    `type` = Option(config.IndividualUserType),
    tier = None,
    segment = None,
    subscription = None,
    email = Option("iu@gmail.com"),
    status = Option(config.InactiveStatuses.head),
    activatedAt = Option(LocalDateTime.now(mockClock)),
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "pegbuser",
    updatedAt = None,
    updatedBy = None)

  "CustomerWalletApplication getWalletApplicationByUserId" should {
    "return Future[Right[WalletApplication]] if wallet applications was found for active user" in {

      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(Some(user)))
      (walletApplicationManagement.getWalletApplicationByUserUuid _).when(customerUuidOne)
        .returns(Future.successful(Right(Set(walletApplicationOne, walletApplicationTwo))))

      val futureResult = customerApplicationWalletService.getWalletApplicationsByUserId(customerUuidOne)

      whenReady(futureResult)(result ⇒ result.right.get mustBe Set(walletApplicationOne, walletApplicationTwo))
    }

    "return Future[Left[InactiveUser]] if user is not active" in {
      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(Some(individualUserInvalid)))

      val futureResult = customerApplicationWalletService.getWalletApplicationsByUserId(customerUuidOne)

      val expectedResult = InactiveCustomerFound(individualUserInvalid)

      whenReady(futureResult) { result ⇒
        result.left.get.message mustBe expectedResult.message
      }
    }

    "return Future[Left[UserNotFound]] if user does not exist" in {
      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(None))

      val futureResult = customerApplicationWalletService.getWalletApplicationsByUserId(customerUuidOne)

      val expectedResult = notFoundError(s"user with uuid $customerUuidOne not found")

      whenReady(futureResult) { result ⇒
        result.left.get.message mustBe expectedResult.message
      }
    }

  }

  "CustomerWalletApplication getWalletApplicationByIdAndUserId" should {
    "return Future[Right[WalletApplication]] if wallet applications was found for active user" in {

      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(Some(user)))
      (walletApplicationManagement.getWalletApplicationById _).when(applicationUuidOne)
        .returns(Future.successful(Right(walletApplicationOne)))

      val futureResult = customerApplicationWalletService
        .getWalletApplicationByApplicationIdAndUserId(customerUuidOne, applicationUuidOne)

      whenReady(futureResult)(result ⇒ result.right.get mustBe walletApplicationOne)
    }

    "return Future[Left[InactiveUser]] if user is not active" in {
      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(Some(individualUserInvalid)))

      val futureResult = customerApplicationWalletService.getWalletApplicationByApplicationIdAndUserId(customerUuidOne, applicationUuidOne)

      val expectedResult = InactiveCustomerFound(individualUserInvalid)

      whenReady(futureResult) { result ⇒
        result.left.get.message mustBe expectedResult.message
      }
    }

    "return Future[Left[UserNotFound]] if user does not exist" in {
      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(None))

      val futureResult = customerApplicationWalletService.getWalletApplicationByApplicationIdAndUserId(customerUuidOne, applicationUuidOne)

      val expectedResult = notFoundError(s"user with uuid $customerUuidOne not found")

      whenReady(futureResult) { result ⇒
        result.left.get.message mustBe expectedResult.message
      }
    }

    "return Future[Left[NotFoundEntity]] if application is not found for relative user" in {
      (userDao.getUser _).when(customerUuidOne.toString).returns(Right(Some(user)))
      (walletApplicationManagement.getWalletApplicationById _).when(applicationUuidTwo)
        .returns(Future.successful(Left(notFoundError(s"wallet application with id $applicationUuidTwo not found"))))

      val futureResult = customerApplicationWalletService.getWalletApplicationByApplicationIdAndUserId(customerUuidOne, applicationUuidTwo)

      val expectedResult = notFoundError(s"wallet application with id $applicationUuidTwo not found")

      whenReady(futureResult) { result ⇒
        result.left.get.message mustBe expectedResult.message
      }
    }

  }

}
