package tech.pegb.backoffice.api.document

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames
import play.api.Configuration
import play.api.inject.bind
import play.api.libs.{Files ⇒ PlayFiles}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto._
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.util.{UUIDLike, Utils, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.model.Ordering.DESCENDING
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService

import scala.concurrent.Future

class DocumentMgmtControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val ec = TestExecutionContext.genericOperations

  val documentMgmt = stub[DocumentManagement]
  val latestVersionService = stub[LatestVersionService]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[DocumentManagement].to(documentMgmt),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockRequestTime = ZonedDateTime.of(2019, 1, 1, 0, 0, 1, 0, Utils.tz) //replaced .now because of unit test assertion problem when response removes trailing zeroes
  override protected lazy val mockRequestFrom = "Admin"
  override protected lazy val mockRequestDate: ZonedDateTime = mockRequestTime
  val mockCustomerId = UUID.randomUUID()
  private val mockApplicationId = UUID.randomUUID()

  "DocumentMgmtController getDocument" should {
    "return Ok 200 and a DocumentToRead json as http response body, all fields returned even null" in {
      val mockDocumentId = UUID.randomUUID()

      val expectedDocument = Document.empty.copy(
        id = mockDocumentId,
        customerId = Some(mockCustomerId),
        applicationId = mockApplicationId.toOption,
        documentType = DocumentTypes.fromString("national_id"),
        status = DocumentStatuses.fromString("pending"),
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

      (documentMgmt.getDocument _)
        .when(mockDocumentId).returns(Future.successful(Right(expectedDocument)))

      val resp = route(
        app,
        FakeRequest(GET, s"/documents/${mockDocumentId}").withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val expectedResponse =
        s"""
           |{
           |"id":"${mockDocumentId}",
           |"customer_id":"${mockCustomerId}",
           |"application_id":"$mockApplicationId",
           |"document_type":"${expectedDocument.documentType}",
           |"document_identifier":null,
           |"purpose":"some purpose",
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"${expectedDocument.createdBy}",
           |"status":"${expectedDocument.status}",
           |"rejection_reason":null,
           |"checked_at":null,
           |"checked_by":null,
           |"uploaded_at":null,
           |"uploaded_by":null,
           |"updated_at":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return NotFound 404 and a message if the document was not found" in {
      val unknownDocumentId = UUID.randomUUID()

      val expectedError = ServiceError.notFoundError(s"Document [$unknownDocumentId] cannot be found.", UUID.randomUUID().toOption)

      (documentMgmt.getDocument _).when(unknownDocumentId).returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest(GET, s"/documents/$unknownDocumentId").withHeaders(jsonHeaders)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe NOT_FOUND

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Document [$unknownDocumentId] cannot be found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return NotFound 404 and a message if the customer related to the document was deactivated" in {
      val mockDocumentId = UUID.randomUUID()

      val expectedError = ServiceError.validationError(
        s"Unable to get document [$mockDocumentId]. Customer owning this document was deactivated.",
        UUID.randomUUID().toOption)

      (documentMgmt.getDocument _).when(mockDocumentId).returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest(GET, s"/documents/$mockDocumentId").withHeaders(jsonHeaders)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to get document [$mockDocumentId]. Customer owning this document was deactivated.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }
  }

  "DocumentMgmtController getDocumentFile" should {
    "return Ok 200 and a stream of bytes in the body" in {
      val mockDocumentId = UUID.randomUUID()

      val expectedDocumentFile: Array[Byte] = "jkbHGFVJHGJKbjBugIU&y7UYiuGYT&^ytGHJB".getBytes

      (documentMgmt.getDocumentFile _)
        .when(mockDocumentId).returns(Future.successful(Right(DocumentFileToRead(mockDocumentId.toString, expectedDocumentFile))))

      val resp = route(
        app,
        FakeRequest(GET, s"/documents/${mockDocumentId}/file").withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "jkbHGFVJHGJKbjBugIU&y7UYiuGYT&^ytGHJB"
    }

    "return NotFound 404 and a message if the document file was not found" in {
      val unknownDocumentId = UUID.randomUUID()

      val expectedError = ServiceError.notFoundError(s"Document file [$unknownDocumentId] cannot be found.", UUID.randomUUID().toOption)

      (documentMgmt.getDocumentFile _).when(unknownDocumentId).returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest(GET, s"/documents/$unknownDocumentId/file").withHeaders(jsonHeaders)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe NOT_FOUND

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Document file [$unknownDocumentId] cannot be found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return NotFound 404 and a message if the customer related to the document file was deactivated" in {
      val mockDocumentId = UUID.randomUUID()

      val expectedError = ServiceError.validationError(
        s"Unable to get document file [$mockDocumentId]. Customer owning this document was deactivated.",
        UUID.randomUUID().toOption)

      (documentMgmt.getDocumentFile _).when(mockDocumentId).returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest(GET, s"/documents/$mockDocumentId/file").withHeaders(jsonHeaders)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to get document file [$mockDocumentId]. Customer owning this document was deactivated.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }
  }

  "DocumentMgmtController getDocumentsByFilters" should {
    val expectedDocuments = Seq(
      Document.empty.copy(
        customerId = Some(mockCustomerId),
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
    "return Ok 200 and a PaginatedResult[DocumentToRead] json, as http response body" in {

      val mockNoCriteria = DocumentCriteria(
        partialMatchFields = Constants.validDocumentMgmntPartialMatchFields.filterNot(_ == "disabled"))
      val ordering = Ordering("created_at", DESCENDING)
      (documentMgmt.countDocumentsByCriteria _).when(mockNoCriteria).returns(Future.successful(Right(expectedDocuments.size)))

      (documentMgmt.getDocumentsByCriteria(_: DocumentCriteria, _: Seq[Ordering], _: Option[Int], _: Option[Int]))
        .when(mockNoCriteria, Seq(ordering), None, None).returns(Future.successful(Right(expectedDocuments)))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(mockNoCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/documents").withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val expectedResponse =
        s"""
           |{"total":3,
           |"results":[
           |{"id":"${expectedDocuments(0).id}",
           |"customer_id":"${expectedDocuments(0).customerId.get}",
           |"application_id":"${expectedDocuments(0).applicationId.get}",
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
           |"uploaded_by":null,
           |"updated_at":null},
           |{"id":"${expectedDocuments(1).id}",
           |"customer_id":"${expectedDocuments(1).customerId.get}",
           |"application_id":"${expectedDocuments(1).applicationId.get}",
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
           |"uploaded_by":"Analyn",
           |"updated_at":null},
           |{"id":"${expectedDocuments(2).id}",
           |"customer_id":"${expectedDocuments(2).customerId.get}",
           |"application_id":"${expectedDocuments(2).applicationId.get}",
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
           |"uploaded_by":"George",
           |"updated_at":null}
           |],
           |"limit":null,
           |"offset":null
           |}
        """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return Ok 200 PaginatedResult[DocumentToRead] meeting the filtering given" in {

      val mockStatusCriteria = DocumentCriteria(
        customerId = Some(UUIDLike.apply(mockCustomerId.toString)),
        status = Option(DocumentStatuses.fromString("pending")),
        partialMatchFields = Constants.validDocumentMgmntPartialMatchFields.filterNot(_ == "disabled"))

      (documentMgmt.countDocumentsByCriteria _).when(mockStatusCriteria).returns(Future.successful(Right(expectedDocuments.filter(_.status.isPending).size)))

      (documentMgmt.getDocumentsByCriteria(_: DocumentCriteria, _: Seq[Ordering], _: Option[Int], _: Option[Int]))
        .when(mockStatusCriteria, *, None, None)
        .returns(Future.successful(Right(expectedDocuments.filter(_.status.isPending))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(mockStatusCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/documents?status=pending&customer_id=$mockCustomerId").withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val expectedResponse =
        s"""
           |{"total":1,
           |"results":[
           |{"id":"${expectedDocuments(0).id}",
           |"customer_id":"$mockCustomerId",
           |"application_id":"${expectedDocuments(0).applicationId.get}",
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
           |"uploaded_by":null,
           |"updated_at":null}
           |],
           |"limit":null,
           |"offset":null
           |}
        """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }

    "return Ok 200 PaginatedResult[DocumentToRead] meeting the given limit and offset" in {
      val mockNoCriteria = DocumentCriteria(partialMatchFields = Constants.validDocumentMgmntPartialMatchFields.filterNot(_ == "disabled"))
      val ordering = Ordering("created_at", DESCENDING)
      (documentMgmt.countDocumentsByCriteria _).when(mockNoCriteria).returns(Future.successful(Right(expectedDocuments.size)))

      (documentMgmt.getDocumentsByCriteria(_: DocumentCriteria, _: Seq[Ordering], _: Option[Int], _: Option[Int]))
        .when(mockNoCriteria, Seq(ordering), Option(1), Option(2))
        .returns(Future.successful(Right(Seq(expectedDocuments(2)))))

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(mockNoCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val resp = route(app, FakeRequest(GET, s"/documents?limit=1&offset=2").withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val expectedResponse =
        s"""
           |{"total":3,
           |"results":[
           |{"id":"${expectedDocuments(2).id}",
           |"customer_id":"${expectedDocuments(2).customerId.get}",
           |"application_id":"${expectedDocuments(2).applicationId.get}",
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
           |"uploaded_by":"George",
           |"updated_at":null}
           |],
           |"limit":1,
           |"offset":2
           |}
        """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
      headers(resp).contains(versionHeaderKey) mustBe true
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion.toOption
    }
  }

  "DocumentMgmtController createDocument" should {
    "return Ok 201 and a DocumentToRead json as http response body" in {
      val mockDocumentToCreate = DocumentToCreate(
        customerId = Some(mockCustomerId),
        applicationId = Some(mockApplicationId),
        fileName = Some("my-selfie.jpg"),
        documentType = DocumentTypes.fromString("selfie"),
        documentIdentifier = None,
        purpose = "for wallet application",
        createdBy = mockRequestFrom,
        createdAt = mockRequestTime.toLocalDateTimeUTC)

      val expectedCreatedDocument = Document.empty.copy(
        customerId = mockDocumentToCreate.customerId,
        documentType = mockDocumentToCreate.documentType,
        documentIdentifier = mockDocumentToCreate.documentIdentifier,
        purpose = mockDocumentToCreate.purpose,
        status = DocumentStatuses.fromString("pending"),
        createdBy = mockDocumentToCreate.createdBy,
        createdAt = mockDocumentToCreate.createdAt)

      (documentMgmt.createDocument _)
        .when(mockDocumentToCreate)
        .returns(Future.successful(Right(expectedCreatedDocument)))

      val jsonRequest =
        s"""
           |{"customer_id":"${mockCustomerId}",
           |"application_id":"$mockApplicationId",
           |"file_name":"my-selfie.jpg",
           |"document_type":"selfie",
           |"document_identifier":null,
           |"purpose":"for wallet application"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, "/documents",
        jsonHeaders,
        jsonRequest)

      val resp = route(app, fakeRequest).get

      val expectedResponse =
        s"""{"id":"${expectedCreatedDocument.id}",
           |"customer_id":"${expectedCreatedDocument.customerId.get}",
           |"application_id":"${expectedCreatedDocument.applicationId.get}",
           |"document_type":"${expectedCreatedDocument.documentType}",
           |"document_identifier":null,
           |"purpose":"${expectedCreatedDocument.purpose}",
           |"created_at":"${expectedCreatedDocument.createdAt.toZonedDateTimeUTC}",
           |"created_by":"$mockRequestFrom",
           |"status":"pending",
           |"rejection_reason":null,
           |"checked_at":null,
           |"checked_by":null,
           |"uploaded_at":null,
           |"uploaded_by":null,
           |"updated_at":null}""".stripMargin.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
      status(resp) mustBe CREATED
    }

    "return BadRequest 400 if request json to create document has missing field even if nullable" in {
      val jsonRequest =
        s"""
           |{"customer_id":"${mockCustomerId}",
           |"application_id":"$mockApplicationId",
           |"document_type":"selfie",
           |"purpose":"for wallet application"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, "/documents",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"MalformedRequest",
           |"msg":"Malformed request to create document. Mandatory field is missing or value of a field is of wrong type."
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 if request json to create document has additional fields that are not recognized" in {
      val jsonRequest =
        s"""
           |{"customer_id":"${mockCustomerId}",
           |"application_id":"$mockApplicationId",
           |"document_type":"selfie",
           |"document_identifier":null,
           |"some_undocumented_field":"51PegasiB",
           |"purpose":"for wallet application"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, "/documents",
        jsonHeaders.add(strictDeserializationKey → "true"),
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get
      status(resp) mustBe BAD_REQUEST
    }

    "return CREATED and ignore additional fields that are not recognized if header to ignore this is present" in {
      val mockDocumentToCreate = DocumentToCreate(
        customerId = Some(mockCustomerId),
        applicationId = Some(mockApplicationId),
        fileName = Some("my-selfie.jpg"),
        documentType = DocumentTypes.fromString("selfie"),
        documentIdentifier = None,
        purpose = "for wallet application",
        createdBy = mockRequestFrom,
        createdAt = mockRequestTime.toLocalDateTimeUTC)

      val expectedCreatedDocument = Document.empty.copy(
        customerId = mockDocumentToCreate.customerId,
        documentType = mockDocumentToCreate.documentType,
        documentIdentifier = mockDocumentToCreate.documentIdentifier,
        purpose = mockDocumentToCreate.purpose,
        status = DocumentStatuses.fromString("pending"),
        createdBy = mockDocumentToCreate.createdBy,
        createdAt = mockDocumentToCreate.createdAt)

      (documentMgmt.createDocument _).when(mockDocumentToCreate).returns(Future.successful(Right(expectedCreatedDocument)))

      val jsonRequest =
        s"""
           |{"customer_id":"${mockCustomerId}",
           |"application_id":"$mockApplicationId",
           |"file_name":"my-selfie.jpg",
           |"document_type":"selfie",
           |"some_undocumented_field_to_be_ignored":"51PegasiB",
           |"another_undocumented_field_to_be_ignored":"Scala Forever",
           |"document_identifier":null,
           |"purpose":"for wallet application"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, "/documents",
        jsonHeaders.add(strictDeserializationKey → "false"),
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedResponse =
        s"""{"id":"${expectedCreatedDocument.id}",
           |"customer_id":"${expectedCreatedDocument.customerId.get}",
           |"application_id":"${expectedCreatedDocument.applicationId.get}",
           |"document_type":"${expectedCreatedDocument.documentType}",
           |"document_identifier":null,
           |"purpose":"${expectedCreatedDocument.purpose}",
           |"created_at":"${expectedCreatedDocument.createdAt.toZonedDateTimeUTC}",
           |"created_by":"$mockRequestFrom",
           |"status":"pending",
           |"rejection_reason":null,
           |"checked_at":null,
           |"checked_by":null,
           |"uploaded_at":null,
           |"uploaded_by":null,
           |"updated_at":null}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document to create has unknown document type" in {
      val mockDocumentToCreate = DocumentToCreate(
        customerId = Some(mockCustomerId),
        applicationId = Some(mockApplicationId),
        fileName = Some("my-selfie.jpg"),
        documentType = DocumentTypes.fromString("graduation certificate"),
        documentIdentifier = None,
        purpose = "for wallet application",
        createdBy = mockRequestFrom,
        createdAt = mockRequestTime.toLocalDateTimeUTC)

      val expectedError = ServiceError.validationError(
        s"Fail to create document. Unknown document type [${mockDocumentToCreate.documentType}]",
        UUID.randomUUID().toOption)
      (documentMgmt.createDocument _).when(mockDocumentToCreate).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        s"""
           |{"customer_id":"${mockCustomerId}",
           |"application_id":"$mockApplicationId",
           |"file_name":"my-selfie.jpg",
           |"document_type":"${mockDocumentToCreate.documentType}",
           |"document_identifier":null,
           |"purpose":"${mockDocumentToCreate.purpose}"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, "/documents",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Fail to create document. Unknown document type [graduation certificate]",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document to create has unknown customer id" in {
      val unknownCustomerId = UUID.randomUUID()
      val mockDocumentToCreate = DocumentToCreate(
        customerId = Some(unknownCustomerId),
        applicationId = Some(mockApplicationId),
        fileName = Some("my-selfie.jpg"),
        documentType = DocumentTypes.fromString("passport"),
        documentIdentifier = None,
        purpose = "for wallet application",
        createdBy = mockRequestFrom,
        createdAt = mockRequestTime.toLocalDateTimeUTC)

      val expectedError = ServiceError.validationError(
        s"Fail to create document. Customer [${mockDocumentToCreate.customerId}] was not found.",
        UUID.randomUUID().toOption)
      (documentMgmt.createDocument _).when(mockDocumentToCreate).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        s"""
           |{"customer_id":"${unknownCustomerId}",
           |"application_id":"$mockApplicationId",
           |"file_name":"my-selfie.jpg",
           |"document_type":"${mockDocumentToCreate.documentType}",
           |"document_identifier":null,
           |"purpose":"${mockDocumentToCreate.purpose}"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, "/documents",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Fail to create document. Customer [${mockDocumentToCreate.customerId}] was not found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }
  }

  val formKeyForFileUpload = app.injector.instanceOf[Configuration].get[String]("document.multipart-form-data.doc-key")
  val formKeyForJson = app.injector.instanceOf[Configuration].get[String]("document.multipart-form-data.json-key")

  "DocumentMgmtController uploadDocumentFile" should {
    val formHeaders = Headers(
      HeaderNames.CONTENT_TYPE -> FORM,
      requestIdHeaderKey → UUID.randomUUID().toString,
      requestDateHeaderKey → mockRequestTime.toString,
      requestFromHeaderKey → mockRequestFrom)
    "return Ok 201 and a DocumentToRead json as http response body" in {
      val mockDocumentId = UUID.randomUUID()
      val expectedDocument = Document.empty.copy(
        id = mockDocumentId,
        customerId = Some(mockCustomerId),
        status = DocumentStatuses.fromString("pending"),
        documentType = DocumentTypes.fromString("selfie"),
        createdAt = mockRequestTime.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        fileUploadedAt = Option(mockRequestTime.toLocalDateTimeUTC),
        fileUploadedBy = Option(mockRequestFrom))

      val updatedAt = ZonedDateTime.now()

      (documentMgmt.uploadDocumentFile(_: UUID, _: Array[Byte], _: String, _: LocalDateTime, _: Option[LocalDateTime]))
        .when(mockDocumentId, *, *, *, Some(updatedAt.toLocalDateTimeUTC))
        .returns(Future.successful(Right(expectedDocument)))

      val fakeRequest = FakeRequest[MultipartFormData[PlayFiles.TemporaryFile]](
        method = "POST",
        uri = s"/documents/$mockDocumentId/file",
        headers = formHeaders,
        body = MultipartFormData[PlayFiles.TemporaryFile](
          dataParts = Map(formKeyForJson → Seq(s"""{"updated_at":"${updatedAt.toString}"}""")),
          files = Seq(FilePart[PlayFiles.TemporaryFile](key = formKeyForFileUpload, filename = "passport.jpg", contentType = None, ref = PlayFiles.SingletonTemporaryFileCreator.create("passport.jpg"))),
          badParts = Seq.empty))

      val controller = inject[DocumentMgmtController]
      val resp = controller.uploadDocumentFile(mockDocumentId).apply(fakeRequest)

      status(resp) mustBe CREATED
    }

    "return BadRequest 404 and a message if the document file to upload has unknown document id" in {
      val unknownDocumentId = UUID.randomUUID()
      val expectedError = ServiceError.notFoundError(s"Unable to upload file. Document id [$unknownDocumentId] was not found.", UUID.randomUUID().toOption)

      (documentMgmt.uploadDocumentFile(_: UUID, _: Array[Byte], _: String, _: LocalDateTime, _: Option[LocalDateTime]))
        .when(unknownDocumentId, *, mockRequestFrom, mockRequestTime.toLocalDateTimeUTC, None)
        .returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest[MultipartFormData[PlayFiles.TemporaryFile]](
        method = "POST",
        uri = s"/documents/$unknownDocumentId/file",
        headers = formHeaders,
        body = MultipartFormData[PlayFiles.TemporaryFile](
          dataParts = Map(formKeyForJson → Seq(s"""{"updated_at":null}""")),
          files = Seq(FilePart[PlayFiles.TemporaryFile](key = formKeyForFileUpload, filename = "passport.jpg", contentType = None, ref = PlayFiles.SingletonTemporaryFileCreator.create("passport.jpg"))),
          badParts = Seq.empty)).withHeaders(jsonHeaders)

      val controller = inject[DocumentMgmtController]
      val resp = controller.uploadDocumentFile(unknownDocumentId).apply(fakeRequest)

      status(resp) mustBe NOT_FOUND

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Unable to upload file. Document id [$unknownDocumentId] was not found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document file to upload is linked to a customer that was deactivated" in {
      val mockDocumentId = UUID.randomUUID()
      val expectedError = ServiceError.validationError(
        s"Unable to upload file. Customer owning this document was deactivated.",
        UUID.randomUUID().toOption)

      (documentMgmt.uploadDocumentFile(_: UUID, _: Array[Byte], _: String, _: LocalDateTime, _: Option[LocalDateTime]))
        .when(mockDocumentId, *, mockRequestFrom, mockRequestTime.toLocalDateTimeUTC, None)
        .returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest[MultipartFormData[PlayFiles.TemporaryFile]](
        method = "POST",
        uri = s"/documents/$mockDocumentId/file",
        headers = formHeaders,
        body = MultipartFormData[PlayFiles.TemporaryFile](
          dataParts = Map(formKeyForJson → Seq(s"""{"updated_at":null}""")),
          files = Seq(FilePart[PlayFiles.TemporaryFile](key = formKeyForFileUpload, filename = "passport.jpg", contentType = None, ref = PlayFiles.SingletonTemporaryFileCreator.create("passport.jpg"))),
          badParts = Seq.empty)).withHeaders(jsonHeaders)

      val controller = inject[DocumentMgmtController]
      val resp = controller.uploadDocumentFile(mockDocumentId).apply(fakeRequest)

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to upload file. Customer owning this document was deactivated.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document file to upload is no longer pending (already approved or rejected)" in {
      val mockDocumentId = UUID.randomUUID()
      val expectedError = ServiceError.validationError(
        s"Unable to upload file. Document was already approved.",
        UUID.randomUUID().toOption)

      (documentMgmt.uploadDocumentFile(_: UUID, _: Array[Byte], _: String, _: LocalDateTime, _: Option[LocalDateTime]))
        .when(mockDocumentId, *, mockRequestFrom, mockRequestTime.toLocalDateTimeUTC, None)
        .returns(Future.successful(Left(expectedError)))

      val fakeRequest = FakeRequest[MultipartFormData[PlayFiles.TemporaryFile]](
        method = "POST",
        uri = s"/documents/$mockDocumentId/file",
        headers = formHeaders,
        body = MultipartFormData[PlayFiles.TemporaryFile](
          dataParts = Map(formKeyForJson → Seq(s"""{"updated_at":null}""")),
          files = Seq(FilePart[PlayFiles.TemporaryFile](key = formKeyForFileUpload, filename = "passport.jpg", contentType = None, ref = PlayFiles.SingletonTemporaryFileCreator.create("passport.jpg"))),
          badParts = Seq.empty)).withHeaders(jsonHeaders)

      val controller = inject[DocumentMgmtController]
      val resp = controller.uploadDocumentFile(mockDocumentId).apply(fakeRequest)

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to upload file. Document was already approved.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }
  }

  "DocumentMgmtController approveDocument" should {
    "return Ok 200 and a DocumentToRead json (status approved, checked_by and checked_at not empty) as http response body" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToApprove = DocumentToApprove(
        id = mockDocumentId,
        approvedBy = mockRequestFrom,
        approvedAt = mockRequestTime.toLocalDateTimeUTC,
        lastUpdatedAt = None)

      val expectedApprovedDocument = Document.empty.copy(
        customerId = Some(UUID.randomUUID()),
        documentType = DocumentTypes.fromString("selfie"),
        documentIdentifier = None,
        purpose = "for wallet application",
        status = DocumentStatuses.fromString("approved"),
        createdBy = mockRequestFrom,
        createdAt = mockRequestTime.toLocalDateTimeUTC,
        checkedBy = Option(mockDocumentToApprove.approvedBy),
        checkedAt = Option(mockDocumentToApprove.approvedAt),
        fileUploadedBy = Option("George"),
        fileUploadedAt = Option(mockRequestTime.plusDays(1).toLocalDateTimeUTC))

      (documentMgmt.approveDocument _).when(mockDocumentToApprove).returns(Future.successful(Right(expectedApprovedDocument)))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/approve", jsonHeaders, jsonRequest)

      val resp = route(app, fakeRequest).get

      val expectedResponse =
        s"""{"id":"${expectedApprovedDocument.id}",
           |"customer_id":"${expectedApprovedDocument.customerId.get}",
           |"application_id":"${expectedApprovedDocument.applicationId.get}",
           |"document_type":"${expectedApprovedDocument.documentType}",
           |"document_identifier":null,
           |"purpose":"${expectedApprovedDocument.purpose}",
           |"created_at":"${expectedApprovedDocument.createdAt.toZonedDateTimeUTC}",
           |"created_by":"${expectedApprovedDocument.createdBy}",
           |"status":"${expectedApprovedDocument.status}",
           |"rejection_reason":null,
           |"checked_at":"${expectedApprovedDocument.checkedAt.get.toZonedDateTimeUTC}",
           |"checked_by":"${expectedApprovedDocument.checkedBy.get}",
           |"uploaded_at":"${mockRequestTime.plusDays(1).toLocalDateTimeUTC.toZonedDateTimeUTC}",
           |"uploaded_by":"George",
           |"updated_at":null}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document to approve is no longer pending" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToApprove = DocumentToApprove(
        id = mockDocumentId,
        approvedBy = mockRequestFrom,
        approvedAt = mockRequestTime.toLocalDateTimeUTC,
        lastUpdatedAt = None)
      val errorId = UUID.randomUUID()
      val expectedError = ServiceError.validationError(
        s"Unable to approve document [$mockDocumentId]. Status is already rejected.",
        errorId.toOption)

      (documentMgmt.approveDocument _).when(mockDocumentToApprove).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/approve", jsonHeaders, jsonRequest)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to approve document [$mockDocumentId]. Status is already rejected.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return NotFound 404 and a message if the document to approve cannot be found" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToApprove = DocumentToApprove(
        id = mockDocumentId,
        approvedBy = mockRequestFrom,
        approvedAt = mockRequestTime.toLocalDateTimeUTC,
        lastUpdatedAt = None)

      val expectedError = ServiceError.notFoundError(s"Unable to approve document [$mockDocumentId]. Document cannot be found.", UUID.randomUUID().toOption)

      (documentMgmt.approveDocument _).when(mockDocumentToApprove).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/approve", jsonHeaders, jsonRequest)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe NOT_FOUND

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Unable to approve document [$mockDocumentId]. Document cannot be found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return NotFound 404 and a message if the document to approve is linked to a customer that is deactivated" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToApprove = DocumentToApprove(
        id = mockDocumentId,
        approvedBy = mockRequestFrom,
        approvedAt = mockRequestTime.toLocalDateTimeUTC,
        lastUpdatedAt = None)

      val expectedError = ServiceError.validationError(
        s"Unable to approve document [$mockDocumentId]. Customer linked to this document is deactivated.",
        UUID.randomUUID().toOption)

      (documentMgmt.approveDocument _).when(mockDocumentToApprove).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/approve", jsonHeaders, jsonRequest)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to approve document [$mockDocumentId]. Customer linked to this document is deactivated.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }
  }

  "DocumentMgmtController rejectDocument" should {
    "return Ok 200 and a DocumentToRead json (status rejected, reason_if_rejected, checked_by and checked_at not empty) as http response body" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToReject = DocumentToReject(
        id = mockDocumentId,
        rejectedBy = mockRequestFrom,
        rejectedAt = mockRequestTime.toLocalDateTimeUTC,
        reason = "document was fake",
        lastUpdatedAt = None)

      val expectedRejectedDocument = Document.empty.copy(
        customerId = Some(UUID.randomUUID()),
        documentType = DocumentTypes.fromString("selfie"),
        documentIdentifier = None,
        purpose = "for wallet application",
        status = DocumentStatuses.fromString("rejected"),
        createdBy = mockRequestFrom,
        createdAt = mockRequestTime.toLocalDateTimeUTC,
        checkedBy = Option(mockDocumentToReject.rejectedBy),
        checkedAt = Option(mockDocumentToReject.rejectedAt),
        rejectionReason = Option(mockDocumentToReject.reason))

      (documentMgmt.rejectDocument _).when(mockDocumentToReject).returns(Future.successful(Right(expectedRejectedDocument)))

      val jsonRequest =
        """
          |{"reason":"document was fake"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/reject", jsonHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedResponse =
        s"""{"id":"${expectedRejectedDocument.id}",
           |"customer_id":"${expectedRejectedDocument.customerId.get}",
           |"application_id":"${expectedRejectedDocument.applicationId.get}",
           |"document_type":"${expectedRejectedDocument.documentType}",
           |"document_identifier":null,
           |"purpose":"${expectedRejectedDocument.purpose}",
           |"created_at":"${expectedRejectedDocument.createdAt.toZonedDateTimeUTC}",
           |"created_by":"${expectedRejectedDocument.createdBy}",
           |"status":"${expectedRejectedDocument.status}",
           |"rejection_reason":"${expectedRejectedDocument.rejectionReason.get}",
           |"checked_at":"${expectedRejectedDocument.checkedAt.get.toZonedDateTimeUTC}",
           |"checked_by":"${expectedRejectedDocument.checkedBy.get}",
           |"uploaded_at":null,
           |"uploaded_by":null,
           |"updated_at":null}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document to reject is no longer pending" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToReject = DocumentToReject(
        id = mockDocumentId,
        rejectedBy = mockRequestFrom,
        rejectedAt = mockRequestTime.toLocalDateTimeUTC,
        reason = "document was fake",
        lastUpdatedAt = None)

      val expectedError = ServiceError.validationError(
        s"Unable to reject document [$mockDocumentId]. Status is already approved.",
        UUID.randomUUID().toOption)

      (documentMgmt.rejectDocument _).when(mockDocumentToReject).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        """
          |{"reason":"document was fake"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/reject", jsonHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to reject document [$mockDocumentId]. Status is already approved.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return NotFound 404 and a message if the document to reject cannot be found" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToReject = DocumentToReject(
        id = mockDocumentId,
        rejectedBy = mockRequestFrom,
        rejectedAt = mockRequestTime.toLocalDateTimeUTC,
        reason = "document was fake",
        lastUpdatedAt = None)

      val expectedError = ServiceError.notFoundError(s"Unable to reject document [$mockDocumentId]. Document cannot be found.", UUID.randomUUID().toOption)

      (documentMgmt.rejectDocument _).when(mockDocumentToReject).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        """
          |{"reason":"document was fake"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/reject", jsonHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe NOT_FOUND

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"Unable to reject document [$mockDocumentId]. Document cannot be found.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }

    "return BadRequest 400 and a message if the document to reject is linked to a customer that is deactivated" in {
      val mockDocumentId = UUID.randomUUID()

      val mockDocumentToReject = DocumentToReject(
        id = mockDocumentId,
        rejectedBy = mockRequestFrom,
        rejectedAt = mockRequestTime.toLocalDateTimeUTC,
        reason = "document was fake",
        lastUpdatedAt = None)

      val expectedError = ServiceError.validationError(
        s"Unable to reject document [$mockDocumentId]. Customer linked to this document is deactivated.",
        UUID.randomUUID().toOption)

      (documentMgmt.rejectDocument _).when(mockDocumentToReject).returns(Future.successful(Left(expectedError)))

      val jsonRequest =
        """
          |{"reason":"document was fake"}
        """.stripMargin.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(PUT, s"/documents/$mockDocumentId/reject", jsonHeaders, jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST

      val expectedResponse =
        s"""
           |{
           |"id":"${mockRequestId}",
           |"code":"InvalidRequest",
           |"msg":"Unable to reject document [$mockDocumentId]. Customer linked to this document is deactivated.",
           |"tracking_id":"${expectedError.id}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedResponse
    }
  }
}
