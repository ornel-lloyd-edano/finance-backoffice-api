package tech.pegb.backoffice.api.proxy

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import akka.util.ByteString
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.libs.{Files ⇒ PlayFiles}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Request, ResponseHeader, Result, _}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, ClaimContent}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, ProxyTestHelper, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class DocumentMgmtProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
  implicit val ec: ExecutionContext = TestExecutionContext.genericOperations
  implicit val tokenExpirationInMinutes: TokenExpiration = TokenExpiration(30)

  val backOfficeUserService: BackOfficeUserService = stub[BackOfficeUserService]
  val proxyResponseHandler: ProxyResponseHandler = stub[ProxyResponseHandler]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[ProxyResponseHandler].to(proxyResponseHandler),
      bind[BackOfficeUserService].to(backOfficeUserService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now: LocalDateTime = LocalDateTime.now(mockClock)
  private val requestDateFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  val passwordService: PasswordService = inject[PasswordService]
  val tokenService: TokenService = inject[TokenService]

  val Endpoint = "documents"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

  "DocumentMgmtProxy routes" should {

    val backofficeUserHeaders = List(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name))

    "forward GET /api/documents/:id to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"documents/$id",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/documents/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/documents/:id/file to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"documents/$id/file",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, "image/jpeg", *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/documents/$id/file")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/documents to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"documents",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/documents")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward POST /api/documents to internal" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"

      val json =
        s"""
           |{
           |  "customer_id": "732e7dfb-f024-4f8a-a21b-3ff6ac1107a8",
           |  "application_id": "c8ad880d-8b4a-4219-be48-2bd5ebc1d245",
           |  "document_type": "national_id",
           |  "document_identifier": "748-1985-2592054-9",
           |  "purpose": "wallet application requirement"
           |}
         """.stripMargin

      val taskBody =
        s"""
           |{
           |"verb":"POST",
           |"url":"$backofficeHost/documents",
           |"body":{"customer_id":"732e7dfb-f024-4f8a-a21b-3ff6ac1107a8",
           |"application_id":"c8ad880d-8b4a-4219-be48-2bd5ebc1d245",
           |"document_type":"national_id",
           |"document_identifier":"748-1985-2592054-9",
           |"purpose":"wallet application requirement"},
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"documents",
           |"action":"create"
           |}
         """.stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(taskBody)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(POST, s"/api/documents",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), json)).get

      status(resp) mustBe OK

    }

    val formKeyForFileUpload = app.injector.instanceOf[Configuration].get[String]("document.multipart-form-data.doc-key")
    val formKeyForJson = app.injector.instanceOf[Configuration].get[String]("document.multipart-form-data.json-key")

    "forward POST /api/documents/:id/file to internal" ignore {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"

      val taskBody =
        s"""
           |{
           |"verb":"POST",
           |"url":"$backofficeHost/documents",
           |"body":{"customer_id":"732e7dfb-f024-4f8a-a21b-3ff6ac1107a8",
           |"application_id":"c8ad880d-8b4a-4219-be48-2bd5ebc1d245",
           |"document_type":"national_id",
           |"document_identifier":"748-1985-2592054-9",
           |"purpose":"wallet application requirement"},
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"documents",
           |"action":"create"
           |}
         """.stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(taskBody)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val fakeRequest = FakeRequest[MultipartFormData[PlayFiles.TemporaryFile]](
        method = "POST",
        uri = s"/documents/$id/file",
        headers = jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")),
        body = MultipartFormData[PlayFiles.TemporaryFile](
          dataParts = Map(formKeyForJson → Seq(s"""{"updated_at":"2019-12-09T07:12:20.574Z"}""")),
          files = Seq(FilePart[PlayFiles.TemporaryFile](key = formKeyForFileUpload, filename = "passport.jpg", contentType = None,
            ref = PlayFiles.SingletonTemporaryFileCreator.create("passport.jpg"))),
          badParts = Seq.empty))

      val controller = inject[DocumentMgmtProxyController]
      val resp = controller.uploadDocumentFile(id).apply(fakeRequest)

      status(resp) mustBe OK

    }

    "forward PUT /api/documents/:id/approve to internal" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        s"""
           |{
           |  "updated_at": "2019-12-09T08:36:31.300Z"
           |}
         """.stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = jsonRequest.some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(*, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/documents/$id/approve")
        .withBody(Json.toJson(jsonRequest))
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward PUT /api/documents/:id/reject to internal" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        s"""
           |{
           |  "reason": "document is fake",
           |  "updated_at": "2019-12-09T08:36:31.300Z"
           |}
         """.stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = jsonRequest.some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(*, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/documents/$id/reject")
        .withBody(Json.toJson(jsonRequest))
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

  }

}
