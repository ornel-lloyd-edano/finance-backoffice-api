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
import play.api.mvc._
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

class WalletApplicationProxyControllerSpec extends PegBNoDbTestApp with ProxyTestHelper with MockFactory with ScalaFutures {
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

  val Endpoint = "wallet_applications"

  val password = "P@ssw0rd123!"
  val hashedPassword = passwordService.hashPassword(password)
  val backOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)

  val claimContent = ClaimContent.from(backOfficeUser)
  val token = tokenService.generateToken(backOfficeUser.userName, claimContent)

  "WalletApplicationProxy " should {
    val backofficeUserHeaders = Seq(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name),
      ("X-ApiKey", apiKey),
      ("request-date", mockRequestDate.toString),
      ("request-id", mockRequestId.toString))

    "forward GET /api/wallet_applications/id to internal" in {
      val id = UUID.randomUUID()
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"wallet_applications/$id",
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

      val resp = route(app, FakeRequest(GET, s"/api/wallet_applications/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/wallet_applications?status=pending to internal" in {
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"wallet_applications",
        body = None,
        queryParameters = Seq(("status", "pending"), ("last_name", "salgado")),
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/wallet_applications?status=pending&last_name=salgado")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/wallet_applications/:id/documents to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"wallet_applications/$id/documents",
        body = None,
        queryParameters = Nil,
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/wallet_applications/$id/documents")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward PUT /api/wallet_applications/:id/approve to internal" in {
      val id = UUID.randomUUID()

      val jsonRequest =
        s"""{
           |"updated_at":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val proxyRequest = ProxyRequest(
        httpMethod = "PUT",
        url = s"wallet_applications/$id/approve",
        body = jsonRequest.some,
        queryParameters = Nil,
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/wallet_applications/$id/approve")
        .withBody(jsonRequest)
        .withHeaders(jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward PUT /api/wallet_applications/:id/reject to internal" in {
      val id = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{"reason":"insufficient document"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val proxyRequest = ProxyRequest(
        httpMethod = "PUT",
        url = s"wallet_applications/$id/reject",
        body = Json.parse(jsonRequest).some,
        queryParameters = Nil,
        headers = backofficeUserHeaders.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("hello"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/wallet_applications/$id/reject")
        .withBody(jsonRequest)
        .withHeaders(jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }
  }

}
