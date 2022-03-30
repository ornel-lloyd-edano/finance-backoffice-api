package tech.pegb.backoffice.api.proxy

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import akka.util.ByteString
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HttpEntity
import play.api.inject.{Binding, bind}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}
import play.libs.Json
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, ClaimContent}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, ProxyTestHelper, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class IndividualUserProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
  implicit val ec: ExecutionContext = TestExecutionContext.genericOperations
  implicit val tokenExpirationInMinutes = TokenExpiration(30)

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

  val EndPoint = "individual_users"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, EndPoint)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

  "IndividualUserProxy routes" should {

    val backofficeUserHeaders = List(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name))

/**************************
           all GET requests
     ***************************/

    "forward GET /api/individual_users/:id to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id",
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

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users/:customerId/accounts to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id/accounts",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id/accounts")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users/:customerId/accounts/:account_id to internal" in {
      val id = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id/accounts/$accountId",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id/accounts/$accountId")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users/:id/wallet_applications to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id/wallet_applications",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id/wallet_applications")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users/:id/wallet_applications/:applicationId to internal" in {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id/wallet_applications/$applicationId",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id/wallet_applications/$applicationId")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users/:customerId/documents to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id/documents",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id/documents")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/individual_users/:customerId/documents/:docId to internal" in {
      val id = UUID.randomUUID()
      val docId = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"individual_users/$id/documents/$docId",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/individual_users/$id/documents/$docId")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

/********************************************************************************
      * all ACTIVATE & DEACTIVATE requests (ignored because does not contain body)
      ****************************************************************************/

    "forward PUT /api/individual_users/:id/activate" ignore {
      val id = UUID.randomUUID()

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/individual_users/$id/activate",
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"individual_users","action":"activate"}""".stripMargin.replaceAll("\n", "").trim

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Json.parse(body).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/individual_users/$id/activate")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }

    "forward DELETE /api/individual_users/:id" ignore {
      val id = UUID.randomUUID()

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/individual_users/$id",
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"individual_users","action":"deactivate"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Json.parse(body.trim).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(DELETE, s"/api/individual_users/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }

/***********************
       all UPDATE requests
     ***********************/

    "forward PUT  /api/individual_users/:id" ignore {
      val id = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{
           |  "id": "$id",
           |  "username": "ujali",
           |  "tier": "test-tier",
           |  "segment": "test-segment",
           |  "subscription": "test-subscription",
           |  "email": "u.tyagi@pegb.tech",
           |  "status": "active",
           |  "msisdn": "random-msisdn",
           |  "individual_user_type": "loner",
           |  "alias": "test_user",
           |  "full_name": "ujali tyagi",
           |  "gender": "female",
           |  "person_id": "1232332",
           |  "document_number": "231212",
           |  "document_type": "passport",
           |  "document_model": "test-model",
           |  "birth_date": "10-03-1989",
           |  "birth_place": "Saharanpur",
           |  "nationality": "Indian",
           |  "occupation": "Software Specialist",
           |  "company_name": "Peg B Technology",
           |  "employer": "Valentina",
           |  "created_at": "2019-12-09T12:37:09.390Z",
           |  "created_by": "lloyd",
           |  "updated_at": "2019-12-09T12:37:09.390Z",
           |  "updated_by": "lloyd",
           |  "activated_at": "2019-12-09T12:37:09.390Z"
           |}
         """.stripMargin.replaceAll("\n", "")

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/individual_users/$id/activate",
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"individual_users","action":"activate"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Json.parse(body.trim).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(*, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/individual_users/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token"))), jsonRequest).get

      status(resp) mustBe OK
    }
  }

}
