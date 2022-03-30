package tech.pegb.backoffice.domain.application

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.Injecting
import tech.pegb.backoffice.dao.application.abstraction.WalletApplicationDao
import tech.pegb.backoffice.dao.application.dto.WalletApplicationToUpdate
import tech.pegb.backoffice.dao.application.entity
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.dao.document.abstraction.{DocumentImmutableFileDao, DocumentTransientFileDao}
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.{HttpClient, ServiceError}
import tech.pegb.backoffice.domain.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.application.model.{ApplicationStatus, WalletApplication}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.DocumentCriteria
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.application.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future
import scala.concurrent.duration._

class WalletApplicationMgmtServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  private val couchbaseDocExpiration = 3.days

  val walletApplicationDao = stub[WalletApplicationDao]
  val userDao = stub[UserDao]
  val httpClientService = stub[HttpClient]
  val documentManagement = stub[DocumentManagement]
  val documentTransientFileDao = stub[DocumentTransientFileDao]
  val documentImmutableFileDao = stub[DocumentImmutableFileDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[WalletApplicationDao].to(walletApplicationDao),
      bind[DocumentManagement].to(documentManagement),
      bind[UserDao].to(userDao),
      bind[HttpClient].to(httpClientService),
      bind[DocumentImmutableFileDao].to(documentImmutableFileDao),
      bind[DocumentTransientFileDao].to(documentTransientFileDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val walletApplicationMgmtService = inject[WalletApplicationManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val applicationUuid = UUID.randomUUID()
  val customerUuidOne = UUID.randomUUID()
  val customerUuidTwo = UUID.randomUUID()

  private val inactiveStatuses: Set[String] = config.InactiveStatuses

  val walletApplication = WalletApplication.getEmpty.copy(
    id = applicationUuid,
    customerId = customerUuidOne,
    fullName = None,
    msisdn = Some(Msisdn(underlying = "+971582181475")),
    status = ApplicationStatus(underlying = "APPROVED"),
    applicationStage = "OCR",
    checkedBy = Some("ujali"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val walletApplicationOne = WalletApplication.getEmpty.copy(
    id = applicationUuid,
    customerId = customerUuidOne,
    fullName = None,
    msisdn = Some(Msisdn(underlying = "+9715821821231")),
    status = ApplicationStatus(underlying = "APPROVED"),
    applicationStage = "OCR",
    checkedBy = Some("test"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val daoWalletApplication = tech.pegb.backoffice.dao.application.entity.WalletApplication.getEmpty.copy(
    id = 1,
    uuid = applicationUuid,
    userUuid = customerUuidOne,
    msisdn = Some("+971582181475"),
    status = "APPROVED",
    applicationStage = "OCR",
    checkedBy = Some("ujali"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val daoWalletApplicationOne = tech.pegb.backoffice.dao.application.entity.WalletApplication.getEmpty.copy(
    id = 1,
    uuid = applicationUuid,
    userUuid = customerUuidOne,
    msisdn = Some("+9715821821231"),
    status = "APPROVED",
    applicationStage = "OCR",
    checkedBy = Some("test"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val daoWalletApplicationTwo = tech.pegb.backoffice.dao.application.entity.WalletApplication.getEmpty.copy(
    id = 1,
    uuid = applicationUuid,
    userUuid = customerUuidTwo,
    msisdn = Some("+971832912112"),
    status = "APPROVED",
    applicationStage = "OCR",
    checkedBy = Some("test"),
    checkedAt = None,
    rejectionReason = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val searchCriteria = WalletApplicationCriteria(
    customerId = Some(customerUuidTwo),
    msisdn = Some(Msisdn(underlying = "+971582181475")),
    name = None,
    fullName = None,
    nationalId = None,
    status = Some(ApplicationStatus(underlying = "APPROVED")),
    applicationStage = Some("OCR"),
    checkedBy = Some("ujali"),
    checkedAtStartingFrom = None,
    checkedAtUpTo = None,
    createdBy = None,
    createdAtStartingFrom = None,
    createdAtUpTo = None)

  val searchCriteriaOne = WalletApplicationCriteria(
    customerId = None,
    msisdn = None,
    name = None,
    fullName = None,
    nationalId = None,
    status = Some(ApplicationStatus(underlying = "APPROVED")),
    applicationStage = Some("OCR"),
    checkedBy = None,
    checkedAtStartingFrom = None,
    checkedAtUpTo = None,
    createdBy = None,
    createdAtStartingFrom = None,
    createdAtUpTo = None)

  val testOrdering = Ordering("createdBy", Ordering.ASCENDING)

  "WalletApplicationMgmt getWalletApplicationById" should {
    "return Future[Right[WalletApplication]] if wallet application was found" in {

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUuid).returns(Right(Some(daoWalletApplication)))

      val futureResult = walletApplicationMgmtService.getWalletApplicationById(applicationUuid)

      whenReady(futureResult)(result ⇒ result.right.get mustBe walletApplication)
    }

    "return Future[Left[NotFound]] if wallet application was not found" in {
      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUuid).returns(Right(None))
      (userDao.getUUIDByInternalUserId _).when(1).returns(Right(Some(customerUuidTwo.toString)))

      val futureResult = walletApplicationMgmtService.getWalletApplicationById(applicationUuid)
      val expectedResponse = ServiceError.notFoundError(s"wallet application with id $applicationUuid not found", UUID.randomUUID().toOption)

      whenReady(futureResult)(result ⇒ result.left.get.message mustBe expectedResponse.message)
    }

  }

  "WalletApplicationMgmt getWalletApplicationsByCriteria" should {
    "return Future[Right[Seq[WalletApplication]]] for all wallet applications of active and waiting_for_activation customers" ignore {
      assert(false)
    }

    "return Future[Right[Seq[WalletApplication]]] for all wallet applications which met the given search criteria" in {
      (walletApplicationDao.getWalletApplicationsByCriteria _).when(
        searchCriteria.asDao(inactiveStatuses), Seq(testOrdering).asDao, Some(1), Some(0)).returns(Right(Seq(daoWalletApplication)))
      (userDao.getUUIDByInternalUserId _).when(1).returns(Right(Some(customerUuidTwo.toString)))

      val futureResult = walletApplicationMgmtService.getWalletApplicationsByCriteria(criteria = searchCriteria, ordering = Seq(testOrdering), limit = Some(1), offset = Some(0))

      whenReady(futureResult)(result ⇒ result.right.get mustBe Seq(walletApplication))
    }

    "return Future[Right[Seq[WalletApplication]]] for a subset of matching wallet applications because of limit and offset" in {
      (walletApplicationDao.getWalletApplicationsByCriteria _).when(
        searchCriteriaOne.asDao(inactiveStatuses), Seq(testOrdering).asDao, Some(1), Some(1)).returns(Right(Seq(daoWalletApplicationOne)))
      (userDao.getUUIDByInternalUserId _).when(1).returns(Right(Some(customerUuidTwo.toString)))

      val futureResult = walletApplicationMgmtService.getWalletApplicationsByCriteria(criteria = searchCriteriaOne, ordering = Seq(testOrdering), limit = Some(1), offset = Some(1))

      whenReady(futureResult)(result ⇒ result.right.get mustBe Seq(walletApplicationOne))
    }
  }

  "WalletApplicationMgmt countWalletApplicationsByCriteria" should {
    "return Future[Right[Int]] which is the total count of all wallet applications which met the given search criteria" in {
      (walletApplicationDao.countWalletApplicationsByCriteria _).when(searchCriteriaOne.asDao(inactiveStatuses)).returns(Right(2))

      val futureResult = walletApplicationMgmtService.countWalletApplicationsByCriteria(criteria = searchCriteriaOne)

      whenReady(futureResult)(result ⇒ result.right.get mustBe 2)
    }

  }

  "WalletApplicationMgmt approvePendingWalletApplication" should {
    "return Future[Right[WalletApplication]] with approved status, checkedBy and checkedAt not empty if success" in {
      val applicationUUID = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val approvedBy = "George"
      val approvedAt = LocalDateTime.now(mockClock)

      val walletApplication = entity.WalletApplication.getEmpty.copy(
        id = 1,
        uuid = applicationUUID,
        userUuid = userUUID,
        msisdn = None,
        status = config.PendingWalletApplicationStatus,
        applicationStage = "scored",
        checkedBy = None,
        checkedAt = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationUUID,
        customerId = userUUID,
        msisdn = None,
        fullName = None,
        status = ApplicationStatus(config.ApprovedWalletApplicationStatus),
        applicationStage = "scored",
        checkedBy = Some(approvedBy),
        checkedAt = Some(approvedAt),
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser",
        updatedAt = Some(approvedAt),
        updatedBy = Some(approvedBy))

      val documentUUID1 = UUID.randomUUID()
      val nationalId = Document(
        id = documentUUID1,
        customerId = Some(userUUID),
        applicationId = applicationUUID.toOption,
        documentName = None,
        documentType = DocumentTypes.fromString(config.NationalIdDocumentType),
        documentIdentifier = None,
        purpose = "application",
        status = DocumentStatuses.fromString(config.DocumentApprovedStatus),
        rejectionReason = None,
        checkedBy = Some(approvedBy),
        checkedAt = Some(approvedAt),
        createdBy = "George",
        createdAt = LocalDateTime.now(mockClock),
        fileUploadedAt = Some(approvedAt),
        fileUploadedBy = Some(approvedBy),
        updatedAt = Some(approvedAt))

      val documentUUID3 = UUID.randomUUID()

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUUID)
        .returns(Right(Some(walletApplication))).noMoreThanOnce()

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUUID)
        .returns(Right(Some(walletApplication.copy(
          status = config.ApprovedWalletApplicationStatus,
          checkedBy = Some(approvedBy),
          checkedAt = Some(approvedAt),
          updatedAt = Some(approvedAt),
          updatedBy = Some(approvedBy)))))

      (httpClientService.request(_: String, _: String, _: Option[JsValue]))
        .when("PATCH", s"${config.CoreWalletApplicationActivationUrl}/${walletApplication.id}", Json.obj("status" → config.ApprovedWalletApplicationStatus, "updated_by" → approvedBy, "last_updated_at" → JsNull).some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      val result = walletApplicationMgmtService.approvePendingWalletApplication(applicationUUID, approvedBy, approvedAt, None)

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isRight mustBe true
        updatedWalletApplication.right.get mustBe expected
      }
    }

    "return Future[Left[NotFound]] if wallet application to approve was not found" in {
      val applicationUUID = UUID.randomUUID()
      val approvedBy = "George"
      val approvedAt = LocalDateTime.now(mockClock)

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUUID)
        .returns(Right(None))

      val result = walletApplicationMgmtService.approvePendingWalletApplication(applicationUUID, approvedBy, approvedAt, None)

      val expectedMessage = s"Approve Wallet Application failed. Wallet application ${applicationUUID} not found."

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isLeft mustBe true
        updatedWalletApplication.left.get.message mustBe expectedMessage
      }
    }

  }

  "WalletApplicationMgmt rejectPendingWalletApplication" should {
    "return Future[Right[WalletApplication]] with rejected status, checkedBy, checkedAt, and rejectionReason not empty if success" in {
      val applicationUUID = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val rejectedBy = "George"
      val rejectedAt = LocalDateTime.now(mockClock)
      val reason = "Incomplete documents"

      val walletApplication = entity.WalletApplication.getEmpty.copy(
        id = 1,
        uuid = applicationUUID,
        userUuid = userUUID,
        msisdn = None,
        status = config.PendingWalletApplicationStatus,
        applicationStage = "scored",
        checkedBy = None,
        checkedAt = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      val walletApplicationToUpdate = WalletApplicationToUpdate(
        status = Some(config.RejectedWalletApplicationStatus),
        checkedAt = Some(rejectedAt),
        checkedBy = Some(rejectedBy),
        updatedAt = rejectedAt,
        updatedBy = rejectedBy,
        rejectionReason = Option(reason))

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationUUID,
        customerId = userUUID,
        msisdn = None,
        status = ApplicationStatus(config.RejectedWalletApplicationStatus),
        applicationStage = "scored",
        checkedBy = Some(rejectedBy),
        checkedAt = Some(rejectedAt),
        rejectionReason = Some(reason),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser",
        updatedAt = Some(rejectedAt),
        updatedBy = Some(rejectedBy))

      val lastUpdatedAt = LocalDateTime.now()
      val user = User.getEmpty.copy(uuid = userUUID.toString, status = Option(config.ActiveUserStatus))

      (httpClientService.request(_: String, _: String, _: Option[JsValue]))
        .when("PATCH", s"${config.CoreWalletApplicationActivationUrl}/${walletApplication.id}", Json.obj("status" → config.RejectedWalletApplicationStatus, "updated_by" → rejectedBy, "rejection_reason" → reason, "last_updated_at" → s"$lastUpdatedAt").some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      (userDao.getUser _).when(walletApplication.userUuid.toString)
        .returns(Right(Some(user)))

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUUID)
        .returns(Right(Some(walletApplication))).noMoreThanOnce()

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUUID)
        .returns(Right(Some(walletApplication.copy(
          status = config.RejectedWalletApplicationStatus,
          checkedBy = Some(rejectedBy),
          checkedAt = Some(rejectedAt),
          updatedAt = Some(rejectedAt),
          updatedBy = Some(rejectedBy),
          rejectionReason = Some(reason)))))

      (userDao.getUUIDByInternalUserId _).when(1).returns(Right(Some(userUUID.toString)))

      val result = walletApplicationMgmtService.rejectPendingWalletApplication(applicationUUID, rejectedBy, rejectedAt, reason, Some(lastUpdatedAt))

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isRight mustBe true
        updatedWalletApplication.right.get mustBe expected
      }
    }

    "return Future[Left[NotFound]] if wallet application to reject was not found" in {
      val applicationUUID = UUID.randomUUID()
      val rejectedBy = "George"
      val rejectedAt = LocalDateTime.now(mockClock)
      val reason = "Incomplete documents"

      (walletApplicationDao.getWalletApplicationByUUID _).when(applicationUUID)
        .returns(Right(None))

      val result = walletApplicationMgmtService.rejectPendingWalletApplication(applicationUUID, rejectedBy, rejectedAt, reason, None)

      val expectedMessage = s"Reject Wallet Application failed. Wallet application ${applicationUUID} not found."

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isLeft mustBe true
        updatedWalletApplication.left.get.message mustBe expectedMessage
      }
    }
  }

  "WalletApplicationMgmt persistApprovedFilesByInternalApplicationId" should {
    "return Future[List[UUID]] of persisted documents" in {
      val userUUID = UUID.randomUUID()
      val applicationUUID = UUID.randomUUID()
      val walletId = 1
      val approvedBy = "George"
      val approvedAt = LocalDateTime.now(mockClock)

      val walletApplication = entity.WalletApplication.getEmpty.copy(
        id = walletId,
        uuid = applicationUUID,
        userUuid = userUUID,
        msisdn = None,
        status = config.ApprovedWalletApplicationStatus,
        applicationStage = "scored",
        checkedBy = Some(approvedBy),
        checkedAt = Some(approvedAt),
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser",
        updatedAt = Some(approvedAt),
        updatedBy = Some(approvedBy))

      val documentUUID1 = UUID.randomUUID()
      val nationalId = Document(
        id = documentUUID1,
        customerId = Some(userUUID),
        applicationId = applicationUUID.toOption,
        documentName = None,
        documentType = DocumentTypes.fromString(config.NationalIdDocumentType),
        documentIdentifier = None,
        purpose = "application",
        status = DocumentStatuses.fromString("APPROVED"),
        rejectionReason = None,
        checkedBy = Some(approvedBy),
        checkedAt = Some(approvedAt),
        createdBy = "George",
        createdAt = LocalDateTime.now(mockClock),
        fileUploadedAt = Some(approvedAt),
        fileUploadedBy = Some(approvedBy),
        updatedAt = Some(approvedAt))

      val documentUUID3 = UUID.randomUUID()
      val selfie = Document(
        id = documentUUID3,
        customerId = Some(userUUID),
        applicationId = applicationUUID.toOption,
        documentName = None,
        documentType = DocumentTypes.fromString(config.SelfieDocumentType),
        documentIdentifier = None,
        purpose = "application",
        status = DocumentStatuses.fromString("APPROVED"),
        rejectionReason = None,
        checkedBy = Some(approvedBy),
        checkedAt = Some(approvedAt),
        createdBy = "George",
        createdAt = LocalDateTime.now(mockClock),
        fileUploadedAt = Some(approvedAt),
        fileUploadedBy = Some(approvedBy),
        updatedAt = Some(approvedAt))

      val documentUUID4 = UUID.randomUUID()
      val liveness = Document(
        id = documentUUID4,
        customerId = Some(userUUID),
        applicationId = applicationUUID.toOption,
        documentName = None,
        documentType = DocumentTypes.fromString(config.LivenessDocumentType),
        documentIdentifier = None,
        purpose = "application",
        status = DocumentStatuses.fromString("APPROVED"),
        rejectionReason = None,
        checkedBy = Some(approvedBy),
        checkedAt = Some(approvedAt),
        createdBy = "George",
        createdAt = LocalDateTime.now(mockClock),
        fileUploadedAt = Some(approvedAt),
        fileUploadedBy = Some(approvedBy),
        updatedAt = Some(approvedAt))

      (walletApplicationDao.getWalletApplicationByInternalId _).when(walletId).returns(Right(Some(walletApplication)))
      (documentManagement.getDocumentsByCriteria _)
        .when(DocumentCriteria(walletApplicationId = Option(applicationUUID), status = Some(DocumentStatuses.fromString(config.DocumentApprovedStatus))), *, None, None)
        .returns(Future.successful(Right(Seq(nationalId, selfie, liveness))))

      //read couchbase
      val mockCouchbaseDocByte = Array(1.toByte, 2.toByte)

      (documentTransientFileDao.readDocumentFile _).when(documentUUID1, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))
      (documentTransientFileDao.readDocumentFile _).when(documentUUID3, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))
      (documentTransientFileDao.readDocumentFile _).when(documentUUID4, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))

      //write hdfs
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID1, mockCouchbaseDocByte, None).returns(Right(documentUUID1))
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID3, mockCouchbaseDocByte, None).returns(Right(documentUUID3))
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID4, mockCouchbaseDocByte, None).returns(Right(documentUUID4))

      val expected = Seq(documentUUID1, documentUUID3, documentUUID4)
      val result = walletApplicationMgmtService.persistApprovedFilesByInternalApplicationId(walletId)

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isRight mustBe true
        updatedWalletApplication.right.get mustBe expected
      }
    }

    "return NotFoundError when application does not exist in db" in {
      val walletId = 1

      (walletApplicationDao.getWalletApplicationByInternalId _).when(walletId).returns(Right(None))

      val expected = s"Persist approved files failed. Wallet Application with id ${walletId} not found"
      val result = walletApplicationMgmtService.persistApprovedFilesByInternalApplicationId(walletId)

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isLeft mustBe true
        updatedWalletApplication.left.get.message mustBe expected
      }
    }
    "return validationError when wallet application is not approved" in {
      val userUUID = UUID.randomUUID()
      val applicationUUID = UUID.randomUUID()
      val walletId = 1

      val walletApplication = entity.WalletApplication.getEmpty.copy(
        id = walletId,
        uuid = applicationUUID,
        userUuid = userUUID,
        msisdn = None,
        status = config.PendingWalletApplicationStatus,
        applicationStage = "scored",
        checkedBy = None,
        checkedAt = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser",
        updatedAt = None,
        updatedBy = None)

      (walletApplicationDao.getWalletApplicationByInternalId _).when(walletId).returns(Right(Some(walletApplication)))

      val expected = s"Persist approved files failed. Wallet with id ${walletId} is not in APPROVED state"
      val result = walletApplicationMgmtService.persistApprovedFilesByInternalApplicationId(walletId)

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isLeft mustBe true
        updatedWalletApplication.left.get.message mustBe expected
      }
    }
  }

}
