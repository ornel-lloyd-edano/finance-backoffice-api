package tech.pegb.backoffice.api.application.controller

import java.time._
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.mvc.Headers
import play.api.test.Helpers.{PUT, route, status, _}
import play.api.test.FakeRequest
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.application.model
import tech.pegb.backoffice.domain.application.model.ApplicationStatus
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.DocumentCriteria
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future
import tech.pegb.backoffice.api.json.Implicits._

class WalletApplicationControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations

  private val walletApplicationManagement = stub[WalletApplicationManagement]
  private val documentsMgmt = stub[DocumentManagement]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[WalletApplicationManagement].to(walletApplicationManagement),
      bind[DocumentManagement].to(documentsMgmt),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  private val mockClock = Clock.fixed(Instant.ofEpochMilli(3000), ZoneId.systemDefault())

  "WalletApplicationController getWalletApplication" should {

    "get a specific wallet application of a customer by application id" in {
      val applicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationId,
        customerId = userUUID,
        msisdn = Some(Msisdn("+1921717784288")),
        fullName = Some("Ujali Test Tyagi"),
        status = ApplicationStatus("APPROVED"),
        applicationStage = "ocr",
        checkedBy = Some("pegbuser"),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = None,
        documentModel = Some("Kenya Identity Card"),
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some("pegbuser"))

      (walletApplicationManagement.getWalletApplicationById _).when(applicationId)
        .returns(Future.successful(Right(expected)))

      val resp = route(app, FakeRequest(GET, s"/wallet_applications/$applicationId")).get

      val expectedJson =
        s"""
           |{
           |"id":"$applicationId",
           |"customer_id":"$userUUID",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"+1921717784288",
           |"status":"approved",
           |"application_stage":"ocr",
           |"applied_at":"1970-01-01T00:00:03Z",
           |"checked_at":"1970-01-01T00:00:03Z",
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null,
           |"total_score":null,
           |"full_name_score":null,
           |"full_name_original":null,
           |"full_name_updated":null,
           |"birthdate_score":null,
           |"birthdate_original":null,
           |"birthdate_updated":null,
           |"birthplace_score":null,
           |"birthplace_original":null,
           |"birthplace_updated":null,
           |"gender_score":null,
           |"gender_original":null,
           |"gender_updated":null,
           |"nationality_score":null,
           |"nationality_original":null,
           |"nationality_updated":null,
           |"person_id_score":null,
           |"person_id_original":null,
           |"person_id_updated":null,
           |"document_number_score":null,
           |"document_number_original":null,
           |"document_number_updated":null,
           |"document_type":null,
           |"document_model":"Kenya Identity Card",
           |"updated_at":${doneAt.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.replaceAll("\n", "")
      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "return 404 NotFound('some message') if application id cannot be found" in {
      val applicationId = UUID.randomUUID()
      val notFoundEntityErrorId = UUID.randomUUID()

      (walletApplicationManagement.getWalletApplicationById _).when(applicationId)
        .returns(Future.successful(
          Left(ServiceError.notFoundError(
            s"wallet application with id $applicationId not found", notFoundEntityErrorId.toOption))))

      val resp = route(app, FakeRequest(GET, s"/wallet_applications/$applicationId").withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"wallet application with id $applicationId not found",
           |"tracking_id":"$notFoundEntityErrorId"}
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expectedJson
    }

    "return 400 BadRequest('some message') if application id cannot be parsed as UUID" in {
      val resp = route(app, FakeRequest(GET, s"/wallet_applications/1232")).get

      val expectedJson =
        s""""code":"Unknown","msg":"Cannot parse parameter id as UUID: Invalid UUID string: 1232"}"""
          .stripMargin.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains expectedJson
    }

    //TODO which applications are considered to deactivated
    "return 400 BadRequest('some message') if customer of that application is deactivated" ignore {
      /* val applicationId = UUID.randomUUID()
      val notFoundEntityErrorId = UUID.randomUUID()

      (walletApplicationManagement.getWalletApplicationById _).when(applicationId)
        .returns(Future.successful(
          Left(ServiceError.notFoundEntityError(
            notFoundEntityErrorId,
            s"wallet application with id $applicationId not found"))))

      val resp = route(app, FakeRequest(GET, s"/wallet_applications/$applicationId")).get

      val expectedJson =
        s"""
           |{"id":"$notFoundEntityErrorId",
           |"code":"NotFound",
           |"msg":"wallet application with id $applicationId not found"}
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expectedJson*/

      assert(false)
    }
  }

  "WalletApplicationController getWalletApplicationsByCriteria" should {
    "get all wallet applications of customers (applications of deactivated customers not included implicitly)" in {
      val applicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)

      val searchCriteria = WalletApplicationCriteria(
        customerId = None,
        msisdn = None,
        name = Some("Ujali"),
        fullName = Some("Tyagi"),
        nationalId = None,
        status = Some(ApplicationStatus(underlying = "APPROVED")),
        applicationStage = None,
        checkedBy = None,
        checkedAtStartingFrom = None,
        checkedAtUpTo = None,
        createdBy = None,
        createdAtStartingFrom = None,
        createdAtUpTo = None)

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationId,
        customerId = userUUID,
        msisdn = Some(Msisdn("+1921717784288")),
        fullName = Some("Ujali Test Tyagi"),
        status = ApplicationStatus("APPROVED"),
        applicationStage = "ocr",
        checkedBy = Some("pegbuser"),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = None,
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some("pegbuser"))

      (walletApplicationManagement
        .countWalletApplicationsByCriteria(
          _: WalletApplicationCriteria))
        .when(searchCriteria)
        .returns(Future.successful(Right(1)))

      (walletApplicationManagement
        .getWalletApplicationsByCriteria(
          _: WalletApplicationCriteria,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int]))
        .when(searchCriteria, Seq.empty[Ordering], None, None)
        .returns(Future.successful(Right(Seq(expected))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(searchCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/wallet_applications?status=APPROVED&name=Ujali&full_name=Tyagi")).get

      val expectedJson =
        s"""{
           |"total":1,
           |"results":[{
           |"id":"$applicationId",
           |"customer_id":"$userUUID",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"+1921717784288",
           |"status":"approved",
           |"application_stage":"ocr",
           |"applied_at":"1970-01-01T00:00:03Z",
           |"checked_at":"1970-01-01T00:00:03Z",
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null,
           |"total_score":null
           |}],
           |"limit":null,
           |"offset":null
           |}
           |""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")

      contentAsString(resp) contains expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get paginated wallet applications if limit and offset query param is given" in {
      val applicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)

      val searchCriteria = WalletApplicationCriteria(
        customerId = None,
        msisdn = None,
        name = Some("Ujali"),
        fullName = Some("Tyagi"),
        nationalId = None,
        status = Some(ApplicationStatus(underlying = "APPROVED")),
        applicationStage = None,
        checkedBy = None,
        checkedAtStartingFrom = None,
        checkedAtUpTo = None,
        createdBy = None,
        createdAtStartingFrom = None,
        createdAtUpTo = None)

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationId,
        customerId = userUUID,
        msisdn = Some(Msisdn("+1921717784288")),
        fullName = Some("Ujali Test Tyagi"),
        status = ApplicationStatus("APPROVED"),
        applicationStage = "ocr",
        checkedBy = Some("pegbuser"),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = None,
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some("pegbuser"))

      (walletApplicationManagement
        .countWalletApplicationsByCriteria(
          _: WalletApplicationCriteria))
        .when(searchCriteria)
        .returns(Future.successful(Right(1)))

      (walletApplicationManagement
        .getWalletApplicationsByCriteria(
          _: WalletApplicationCriteria,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int]))
        .when(searchCriteria, Seq.empty[Ordering], Some(1), Some(1))
        .returns(Future.successful(Right(Seq(expected))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(searchCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/wallet_applications?status=APPROVED&name=Ujali&full_name=Tyagi&limit=1&offset=1")).get

      val expectedJson =
        s"""{
           |"total":1,
           |"results":[{
           |"id":"$applicationId",
           |"customer_id":"$userUUID",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"+1921717784288",
           |"status":"approved",
           |"application_stage":"ocr",
           |"applied_at":"1970-01-01T00:00:03Z",
           |"checked_at":"1970-01-01T00:00:03Z",
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null
           |"total_score":null
           |}],
           |"limit":1,
           |"offset":1
           |}
           |""".stripMargin.replaceAll("\n", "")
      contentAsString(resp) contains expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get all documents by application" in {
      val mockApplicationId = UUID.randomUUID()

      val expectedDocumentCriteria = DocumentCriteria(walletApplicationId = Option(mockApplicationId))

      val expectedDocuments = Seq(
        Document.empty.copy(
          documentType = DocumentTypes.fromString("national_id"),
          status = DocumentStatuses.fromString("pending"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0)),
        Document.empty.copy(
          documentType = DocumentTypes.fromString("selfie"),
          status = DocumentStatuses.fromString("approved"),
          checkedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0)),
          checkedBy = Option("George"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
          fileUploadedBy = Option("Analyn"),
          fileUploadedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0))),
        Document.empty.copy(
          documentType = DocumentTypes.fromString("passport"),
          status = DocumentStatuses.fromString("rejected"),
          rejectionReason = Option("fake document"),
          checkedBy = Option("Analyn"),
          checkedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0)),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
          fileUploadedBy = Option("George"),
          fileUploadedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0))))

      (documentsMgmt.getDocumentsByCriteria(_: DocumentCriteria, _: Seq[Ordering], _: Option[Int], _: Option[Int]))
        .when(expectedDocumentCriteria, *, None, None).returns(Future.successful(Right(expectedDocuments)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(expectedDocumentCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/wallet_applications/$mockApplicationId/documents")).get

      status(resp) mustBe OK

      val expectedResponse =
        s"""
           |{"total":3,
           |"results":[
           |{"id":"${expectedDocuments(0).id}",
           |"customer_id":"${expectedDocuments(0).customerId}",
           |"document_type":"national_id",
           |"document_identifier":null,
           |"purpose":"some purpose",
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"some user",
           |"status":"pending",
           |"rejection_reason":null,
           |"checked_at":null,
           |"checked_by":null,
           |"uploaded_at":null,
           |"uploaded_by":null},
           |{"id":"${expectedDocuments(1).id}",
           |"customer_id":"${expectedDocuments(1).customerId}",
           |"document_type":"selfie",
           |"document_identifier":null,
           |"purpose":"some purpose",
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"some user",
           |"status":"approved",
           |"rejection_reason":null,
           |"checked_at":"2019-02-01T00:00:00Z",
           |"checked_by":"George",
           |"uploaded_at":"2019-02-01T00:00:00Z",
           |"uploaded_by":"Analyn"},
           |{"id":"${expectedDocuments(2).id}",
           |"customer_id":"${expectedDocuments(2).customerId}",
           |"document_type":"passport",
           |"document_identifier":null,
           |"purpose":"some purpose",
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"some user",
           |"status":"rejected",
           |"rejection_reason":"fake document",
           |"checked_at":"2019-02-01T00:00:00Z",
           |"checked_by":"Analyn",
           |"uploaded_at":"2019-02-01T00:00:00Z",
           |"uploaded_by":"George"}
           |],
           |"limit":null,
           |"offset":null
           |}
        """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      contentAsString(resp) contains expectedResponse
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return count=0 and results=[] in get all documents by application if HEAD method is used" in {
      val mockApplicationId = UUID.randomUUID()

      val expectedDocumentCriteria = DocumentCriteria(walletApplicationId = Option(mockApplicationId))

      val expectedDocuments = Seq(
        Document.empty.copy(
          documentType = DocumentTypes.fromString("national_id"),
          status = DocumentStatuses.fromString("pending"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0)),
        Document.empty.copy(
          documentType = DocumentTypes.fromString("selfie"),
          status = DocumentStatuses.fromString("approved"),
          checkedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0)),
          checkedBy = Option("George"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
          fileUploadedBy = Option("Analyn"),
          fileUploadedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0))),
        Document.empty.copy(
          documentType = DocumentTypes.fromString("passport"),
          status = DocumentStatuses.fromString("rejected"),
          rejectionReason = Option("fake document"),
          checkedBy = Option("Analyn"),
          checkedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0)),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
          fileUploadedBy = Option("George"),
          fileUploadedAt = Option(LocalDateTime.of(2019, 2, 1, 0, 0, 0))))

      (documentsMgmt.getDocumentsByCriteria(_: DocumentCriteria, _: Seq[Ordering], _: Option[Int], _: Option[Int]))
        .when(expectedDocumentCriteria, *, None, None).returns(Future.successful(Right(expectedDocuments)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(expectedDocumentCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(HEAD, s"/wallet_applications/$mockApplicationId/documents")).get

      status(resp) mustBe OK

      val expectedResponse =
        s"""
           |{"total":3,
           |"results":[
           |],
           |"limit":null,
           |"offset":null
           |}
        """.stripMargin.trim.replaceAll(System.lineSeparator(), "")
      contentAsString(resp) contains expectedResponse
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get all pending wallet applications during a specified date range" in {
      val applicationIdOne = UUID.randomUUID()
      val userUUIDOne = UUID.randomUUID()
      val applicationIdTwo = UUID.randomUUID()
      val userUUIDTwo = UUID.randomUUID()

      val doneAt = ZonedDateTime.now(mockClock)

      val startDate = LocalDate.of(2019, 1, 1)
      val endDate = LocalDate.of(2019, 1, 30)

      val searchCriteria = WalletApplicationCriteria(
        customerId = None,
        msisdn = None,
        name = None,
        fullName = None,
        nationalId = None,
        status = Some(ApplicationStatus(underlying = "PENDING")),
        applicationStage = None,
        checkedBy = None,
        checkedAtStartingFrom = None,
        checkedAtUpTo = None,
        createdBy = None,
        createdAtStartingFrom = Some(startDate),
        createdAtUpTo = Some(endDate))

      val expected = Seq(
        model.WalletApplication.getEmpty.copy(
          id = applicationIdOne,
          customerId = userUUIDOne,
          msisdn = Some(Msisdn("+1921717784288")),
          fullName = Some("Ujali Test Tyagi"),
          status = ApplicationStatus("PENDING"),
          applicationStage = "ocr",
          checkedBy = Some("pegbuser"),
          checkedAt = None,
          rejectionReason = None,
          createdAt = startDate.atTime(9, 53, 57),
          createdBy = "pegbuser",
          updatedAt = Some(doneAt.toLocalDateTimeUTC),
          updatedBy = Some("pegbuser")),

        model.WalletApplication.getEmpty.copy(
          id = applicationIdTwo,
          customerId = userUUIDTwo,
          msisdn = Some(Msisdn("+1912234242214")),
          fullName = Some("Dima Test Linou"),
          status = ApplicationStatus("PENDING"),
          applicationStage = "ocr",
          checkedBy = Some("pegbuser"),
          checkedAt = None,
          rejectionReason = None,
          createdAt = endDate.atTime(9, 53, 57),
          createdBy = "pegbuser",
          updatedAt = Some(doneAt.toLocalDateTimeUTC),
          updatedBy = Some("pegbuser")))

      (walletApplicationManagement
        .countWalletApplicationsByCriteria(
          _: WalletApplicationCriteria))
        .when(searchCriteria)
        .returns(Future.successful(Right(2)))

      (walletApplicationManagement
        .getWalletApplicationsByCriteria(
          _: WalletApplicationCriteria,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int]))
        .when(searchCriteria, Seq.empty[Ordering], None, None)
        .returns(Future.successful(Right(expected)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(searchCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(
        app,
        FakeRequest(GET, s"/wallet_applications?status=PENDING&start_date=$startDate&end_date=$endDate")).get

      val expectedJson =
        s"""
           |{
           |"total": 2,
           |"results":[{
           |"id":"$applicationIdOne",
           |"customer_id":"$userUUIDOne",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"+1921717784288",
           |"status":"pending",
           |"application_stage":"ocr",
           |"applied_at":"2019-01-01T09:53:57Z",
           |"checked_at":null,
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null,
           |"total_score":null
           |},
           |{
           |"id":"$applicationIdTwo",
           |"customer_id":"$userUUIDTwo",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"+1912234242214",
           |"status":"pending",
           |"application_stage":"ocr",
           |"applied_at":"2019-01-30T09:53:57Z",
           |"checked_at":null,
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null,
           |"total_score":null
           |}],
           |"limit":null,
           |"offset":null
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll(" ", "")

      contentAsString(resp) contains expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get all wallet applications from a given msisdn" in {
      val applicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)

      val searchCriteria = WalletApplicationCriteria(
        customerId = None,
        msisdn = Some(Msisdn("987121323423")),
        name = None,
        fullName = None,
        nationalId = None,
        status = None,
        applicationStage = None,
        checkedBy = None,
        checkedAtStartingFrom = None,
        checkedAtUpTo = None,
        createdBy = None,
        createdAtStartingFrom = None,
        createdAtUpTo = None)

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationId,
        customerId = userUUID,
        msisdn = Some(Msisdn("987121323423")),
        fullName = Some("Ujali Test Tyagi"),
        status = ApplicationStatus("APPROVED"),
        applicationStage = "ocr",
        checkedBy = Some("pegbuser"),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = None,
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some("pegbuser"))

      (walletApplicationManagement
        .countWalletApplicationsByCriteria(
          _: WalletApplicationCriteria))
        .when(searchCriteria)
        .returns(Future.successful(Right(1)))

      (walletApplicationManagement
        .getWalletApplicationsByCriteria(
          _: WalletApplicationCriteria,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int]))
        .when(searchCriteria, Seq.empty[Ordering], None, None)
        .returns(Future.successful(Right(Seq(expected))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(searchCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/wallet_applications?msisdn=987121323423")).get

      val expectedJson =
        s"""{
           |"total":1,
           |"results":[{
           |"id":"$applicationId",
           |"customer_id":"$userUUID",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"987121323423",
           |"status":"approved",
           |"application_stage":"ocr",
           |"applied_at":"1970-01-01T00:00:03Z",
           |"checked_at":"1970-01-01T00:00:03Z",
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null,
           |"total_score":null
           |}],
           |"limit":null,
           |"offset":null
           |}
           |""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")
      contentAsString(resp) contains expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get all wallet applications from a given first name and last name" in {
      val applicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)

      val searchCriteria = WalletApplicationCriteria(
        customerId = None,
        msisdn = None,
        name = Some("Ujali"),
        fullName = Some("Tyagi"),
        nationalId = None,
        status = None,
        applicationStage = None,
        checkedBy = None,
        checkedAtStartingFrom = None,
        checkedAtUpTo = None,
        createdBy = None,
        createdAtStartingFrom = None,
        createdAtUpTo = None)

      val expected = model.WalletApplication.getEmpty.copy(
        id = applicationId,
        customerId = userUUID,
        msisdn = Some(Msisdn("+1921717784288")),
        fullName = Some("Ujali Test Tyagi"),
        status = ApplicationStatus("APPROVED"),
        applicationStage = "ocr",
        checkedBy = Some("pegbuser"),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = None,
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some("pegbuser"))

      (walletApplicationManagement
        .countWalletApplicationsByCriteria(
          _: WalletApplicationCriteria))
        .when(searchCriteria)
        .returns(Future.successful(Right(1)))

      (walletApplicationManagement
        .getWalletApplicationsByCriteria(
          _: WalletApplicationCriteria,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int]))
        .when(searchCriteria, Seq.empty[Ordering], None, None)
        .returns(Future.successful(Right(Seq(expected))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(searchCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/wallet_applications?name=Ujali&full_name=Tyagi")).get

      val expectedJson =
        s"""{
           |"total":1,
           |"results":[{
           |"id":"$applicationId",
           |"customer_id":"$userUUID",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":"+1921717784288",
           |"status":"approved",
           |"application_stage":"ocr",
           |"applied_at":"1970-01-01T00:00:03Z",
           |"checked_at":"1970-01-01T00:00:03Z",
           |"checked_by":"pegbuser",
           |"reason_if_rejected":null,
           |"total_score":null
           |}],
           |"limit":null,
           |"offset":null
           |}
           |""".stripMargin.replaceAll("\n", "").replaceAll(" ", "")
      contentAsString(resp) contains expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "get empty results if no applications match the given criteria" in {
      val searchCriteria = WalletApplicationCriteria(
        customerId = None,
        msisdn = None,
        name = Some("Ujali"),
        fullName = Some("Tyagi"),
        nationalId = None,
        status = None,
        applicationStage = None,
        checkedBy = None,
        checkedAtStartingFrom = None,
        checkedAtUpTo = None,
        createdBy = None,
        createdAtStartingFrom = None,
        createdAtUpTo = None)

      (walletApplicationManagement
        .countWalletApplicationsByCriteria(
          _: WalletApplicationCriteria))
        .when(searchCriteria)
        .returns(Future.successful(Right(0)))

      (walletApplicationManagement
        .getWalletApplicationsByCriteria(
          _: WalletApplicationCriteria,
          _: Seq[Ordering],
          _: Option[Int],
          _: Option[Int]))
        .when(searchCriteria, Seq.empty[Ordering], None, None)
        .returns(Future.successful(Right(Seq.empty)))

      val mockLatestVersion = ""
      (latestVersionService.getLatestVersion _).when(searchCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/wallet_applications?name=Ujali&full_name=Tyagi")).get

      val expectedJson =
        s"""{
           |"total":0,
           |"results":[],
           |"limit":null,
           |"offset":null
           |}
           |""".stripMargin.replaceAll("\n", "")

      contentAsString(resp) mustBe expectedJson
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return 400 BadRequest('some message') if given msisdn is not msisdn format" in {
      val resp = route(app, FakeRequest(GET, s"/wallet_applications?msisdn=1231")).get

      val expectedJson =
        s"""
           |"code":"MalformedRequest",
           |"msg":"assertion failed: invalid Msisdn: 1231"
         """.stripMargin

      contentAsString(resp).contains(expectedJson)
    }

    //TODO add test case when NameAttribute is enbaled
    "return 400 BadRequest('some message') if given first name or last name is not human name format" ignore {
      val resp = route(app, FakeRequest(GET, s"/wallet_applications?firstName=1231&firstName=3214")).get

      val expectedJson = """"msg":"assertion failed: invalid Msisdn: 1231"}"""

      contentAsString(resp).contains(expectedJson) mustBe true
    }
  }

  "WalletApplicationController updateWalletApplication" should {

    "approve a pending wallet application and return Ok(approved application)" in {
      val pendingApplicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "pegbuser"

      val expected = model.WalletApplication.getEmpty.copy(
        id = pendingApplicationId,
        customerId = userUUID,
        msisdn = None,
        fullName = Some("Dima Test Linou"),
        status = ApplicationStatus("APPROVED"),
        applicationStage = "scored",
        checkedBy = Some(doneBy),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = None,
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some(doneBy))

      (walletApplicationManagement.approvePendingWalletApplication _).when(pendingApplicationId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future.successful(Right(expected)))

      val jsonRequest =
        s"""{
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"/wallet_applications/${pendingApplicationId}/approve", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
            |{"id":"$pendingApplicationId",
            |"customer_id":"$userUUID",
            |"full_name":"Ujali Test Tyagi",
            |"person_id":null,
            |"msisdn":null,
            |"status":"approved",
            |"application_stage":"scored",
            |"applied_at":"1970-01-01T00:00:03Z",
            |"checked_at":"1970-01-01T00:00:03Z",
            |"checked_by":"pegbuser",
            |"reason_if_rejected":null,
            |"total_score":null}
            |""".stripMargin.replaceAll("\n", "")
      status(resp) mustBe OK
      contentAsString(resp) contains expectedJson

    }

    "reject a pending wallet application and return Ok(rejected application)" in {
      val pendingApplicationId = UUID.randomUUID()
      val userUUID = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "pegbuser"

      val expected = model.WalletApplication.getEmpty.copy(
        id = pendingApplicationId,
        customerId = userUUID,
        msisdn = None,
        fullName = Some("Dima Test Linou"),
        status = ApplicationStatus("REJECTED"),
        applicationStage = "scored",
        checkedBy = Some(doneBy),
        checkedAt = Some(doneAt.toLocalDateTimeUTC),
        rejectionReason = Some("insufficient document"),
        createdAt = ZonedDateTime.now(mockClock).toLocalDateTimeUTC,
        createdBy = "pegbuser",
        updatedAt = Some(doneAt.toLocalDateTimeUTC),
        updatedBy = Some(doneBy))

      val jsonRequest =
        s"""
           |{"reason": "insufficient document"}
        """.stripMargin.replaceAll("\n", "")

      (walletApplicationManagement.rejectPendingWalletApplication _)
        .when(pendingApplicationId, doneBy, doneAt.toLocalDateTimeUTC, "insufficient document", None)
        .returns(Future.successful(Right(expected)))

      val resp = route(app, FakeRequest(PUT, s"/wallet_applications/$pendingApplicationId/reject")
        .withBody(jsonRequest)
        .withHeaders(Headers(Seq("Content-type" → "application/json", requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → UUID.randomUUID().toString): _*))).get

      val expectedJson =
        s"""
           |{"id":"$pendingApplicationId",
           |"customer_id":"$userUUID",
           |"full_name":"Ujali Test Tyagi",
           |"person_id":null,
           |"msisdn":null,
           |"status":"rejected",
           |"application_stage":"scored",
           |"applied_at":"1970-01-01T00:00:03Z",
           |"checked_at":"1970-01-01T00:00:03Z",
           |"checked_by":"pegbuser",
           |"reason_if_rejected":"insufficient document",
           |"total_score":null}
           |""".stripMargin.replaceAll("\n", "")
      status(resp) mustBe OK
      contentAsString(resp) contains expectedJson
    }

    "return BadRequest('some message') if approving an application that is not pending" in {
      val applicationId = UUID.randomUUID()
      val validationErrorId = UUID.randomUUID()

      (walletApplicationManagement.approvePendingWalletApplication _)
        .when(applicationId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future.successful(Left(ServiceError.validationError(s"Approve Wallet Application failed. Wallet application $applicationId is not in PENDING state.", validationErrorId.toOption))))

      val jsonRequest =
        s"""{
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"/wallet_applications/${applicationId}/approve", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Approve Wallet Application failed. Wallet application $applicationId is not in PENDING state.",
           |"tracking_id":"$validationErrorId"}
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "return BadRequest('some message') if rejecting an application that is not pending" in {
      val applicationId = UUID.randomUUID()
      val validationErrorId = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "pegbuser"

      val jsonRequest =
        s"""
           |{"reason": "insufficient document"}
        """.stripMargin.replaceAll("\n", "")

      (walletApplicationManagement.rejectPendingWalletApplication _)
        .when(applicationId, doneBy, doneAt.toLocalDateTimeUTC, "insufficient document", None)
        .returns(Future.successful(Left(ServiceError.validationError(s"Reject Wallet Application failed. Wallet application $applicationId is not in PENDING state.", validationErrorId.toOption))))

      val resp = route(app, FakeRequest(PUT, s"/wallet_applications/${applicationId}/reject")
        .withBody(jsonRequest)
        .withHeaders(Headers(Seq("Content-type" → "application/json", requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → mockRequestId.toString): _*))).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Reject Wallet Application failed. Wallet application $applicationId is not in PENDING state.",
           |"tracking_id":"$validationErrorId"}
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "return BadRequest('some message') if approving an application of deactivated customer" in {
      val applicationId = UUID.randomUUID()
      val validationErrorId = UUID.randomUUID()

      (walletApplicationManagement.approvePendingWalletApplication _)
        .when(applicationId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future.successful(Left(ServiceError.validationError(s"Customer was found but not active", validationErrorId.toOption))))

      val jsonRequest =
        s"""{
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"/wallet_applications/${applicationId}/approve", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Customer was found but not active",
           |"tracking_id":"$validationErrorId"}
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "return BadRequest('some message') if rejecting an application of deactivated customer" in {
      val applicationId = UUID.randomUUID()
      val validationErrorId = UUID.randomUUID()
      val doneAt = ZonedDateTime.now(mockClock)
      val doneBy = "pegbuser"

      val jsonRequest =
        s"""
           |{"reason": "insufficient document"}
        """.stripMargin.replaceAll("\n", "")

      (walletApplicationManagement.rejectPendingWalletApplication _)
        .when(applicationId, doneBy, doneAt.toLocalDateTimeUTC, "insufficient document", None)
        .returns(Future.successful(Left(ServiceError.validationError(s"Customer was found but not active", validationErrorId.toOption))))

      val resp = route(app, FakeRequest(PUT, s"/wallet_applications/${applicationId}/reject")
        .withBody(jsonRequest)
        .withHeaders(Headers(Seq("Content-type" → "application/json", requestDateHeaderKey → doneAt.toString, requestFromHeaderKey → doneBy, "request-id" → mockRequestId.toString): _*))).get

      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Customer was found but not active",
           |"tracking_id":"$validationErrorId"}
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
  }

}
