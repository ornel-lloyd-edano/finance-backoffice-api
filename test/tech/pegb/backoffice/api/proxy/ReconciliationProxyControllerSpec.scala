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
import play.api.libs.json.Json
import play.api.mvc.{Request, ResponseHeader, Result}
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

class ReconciliationProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
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

  val EndPoint = "internal_recons"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, EndPoint)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

  val backofficeUserHeaders = List(
    ("X-UserName", backOfficeUser.userName),
    ("X-RoleLevel", backOfficeUser.role.level.toString),
    ("X-BusinessUnit", backOfficeUser.businessUnit.name))

  "ReconciliationProxy routes" should {

    "forward GET /api/internal_recons/:id to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"internal_recons/$id",
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

      val resp = route(app, FakeRequest(GET, s"/api/internal_recons/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }

    "forward GET /api/internal_recons to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"internal_recons",
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

      val resp = route(app, FakeRequest(GET, s"/api/internal_recons")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }

    "forward GET /api/internal_recons/incidents to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"internal_recons/incidents",
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

      val resp = route(app, FakeRequest(GET, s"/api/internal_recons/incidents")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }
  }

  "forward PUT /api/internal_recons/:id to internal" in {
    val id = UUID.randomUUID()
    val backofficeHost = "$backoffice_api_host"
    val jsonBody =
      """
        |{
        |  "comments": "Manual transaction created. resolving...",
        |  "updated_at": "2019-12-10T13:28:54.296Z"
        |}
      """.stripMargin

    val taskBody =
      s"""
         |{
         |"verb":"PUT",
         |"url":"$backofficeHost/internal_recons/$id",
         |"body":$jsonBody,
         |"headers":{"request-id":"${mockRequestId.toString}",
         |"X-UserName":"scala.user"},
         |"module":"internal_recons",
         |"action":"update"
         |}
         """.stripMargin

    val proxyRequest = ProxyRequest(
      httpMethod = "POST",
      url = s"tasks",
      body = Json.parse(taskBody).some,
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

    val resp = route(app, FakeRequest(PUT, s"/api/internal_recons/$id",
      jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonBody)).get

    status(resp) mustBe OK
  }
}
