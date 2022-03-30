package tech.pegb.backoffice.api.proxy

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
import play.api.test.Helpers._
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.ClaimContent
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, ProxyTestHelper, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class ScopeProxyControllerSpec extends PegBNoDbTestApp with ProxyTestHelper with MockFactory with ScalaFutures {
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
  val now = LocalDateTime.now(mockClock)

  val passwordService = inject[PasswordService]
  val tokenService = inject[TokenService]

  val Endpoint = "scopes"

  val password = "P@ssw0rd123!"
  val hashedPassword = passwordService.hashPassword(password)
  val backOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)

  val claimContent = ClaimContent.from(backOfficeUser)
  val token = tokenService.generateToken(backOfficeUser.userName, claimContent)

  "ScopeProxy" should {
    val backofficeUserHeaders = Seq(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name),
      ("X-ApiKey", apiKey),
      ("request-date", mockRequestDate.toString),
      ("request-id", mockRequestId.toString))

    "forward GET /api/scopes/id to internal" in {
      val id = UUID.randomUUID()
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"scopes/$id",
        body = None,
        queryParameters = Seq.empty,
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/scopes/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/scopes?name=accounts to internal" in {
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"scopes",
        body = None,
        queryParameters = Seq(("name", "accounts")),
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/scopes?name=accounts")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward POST /api/scopes to internal" in {
      val jsonRequest =
        s"""{
           |"name":"accounts scope_scope",
           |"parent_id":null,
           |"description":"Some description for accounts scope"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"scopes",
        body = jsonRequest.some,
        queryParameters = Seq.empty,
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(POST, s"/api/scopes")
        .withJsonBody(Json.parse(jsonRequest))
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }
  }
}
