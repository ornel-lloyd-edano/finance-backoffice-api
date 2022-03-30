package tech.pegb.backoffice.api.proxy

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import akka.util.ByteString
import cats.implicits._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HttpEntity
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.mvc.{Request, ResponseHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, PUT, route, status, _}
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, ClaimContent}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, ProxyTestHelper, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class CurrencyExchangeProxyControllerSpec extends PegBNoDbTestApp with ScalaFutures with ProxyTestHelper {
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

  val Endpoint = "currency_exchanges"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

  "CurrencyExchangeProxy routes" should {

    val backofficeUserHeaders = List(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name))

    "forward POST /api/currency_exchanges/:id/spreads to internal" in {
      val id = UUID.randomUUID()
      val jsonRequest =
        """
          |{
          |  "transaction_type": "currency_exchange",
          |  "channel": "test",
          |  "institution": "test",
          |  "spread": 1
          |}
        """.stripMargin.replaceAll("\n", "")

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"POST",
           |"url":"$backofficeHost/currency_exchanges/$id/spreads",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"create"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(body)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))
      val resp = route(app, FakeRequest(POST, s"/api/currency_exchanges/$id/spreads", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward GET /api/currency_exchanges to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"currency_exchanges",
        body = None,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward PUT /api/currency_exchanges/:fxId/spreads/:spreadId to internal" in {
      val fxId = UUID.randomUUID()
      val spreadsId = UUID.randomUUID()
      val jsonRequest =
        """
          |{
          |  "spread": 2,
          |  "updated_at": "2019-11-20T07:40:56.004Z"
          |}
        """.stripMargin.replaceAll("\n", "")

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/currency_exchanges/$fxId/spreads/$spreadsId",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"update"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(body)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))
      val resp = route(app, FakeRequest(PUT, s"/api/currency_exchanges/$fxId/spreads/$spreadsId", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward DELETE /api/currency_exchanges/:fxId/spreads/:spreadId to internal" in {
      val fxId = UUID.randomUUID()
      val spreadsId = UUID.randomUUID()
      val jsonRequest =
        """
          |{
          |  "updated_at": "2019-11-20T07:59:26.370Z"
          |}
        """.stripMargin.replaceAll("\n", "")

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"DELETE",
           |"url":"$backofficeHost/currency_exchanges/$fxId/spreads/$spreadsId",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"delete"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(body)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))
      val resp = route(app, FakeRequest(DELETE, s"/api/currency_exchanges/$fxId/spreads/$spreadsId", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward PUT /api/currency_exchanges/activate" in {

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/currency_exchanges/activate",
           |"body":{"updated_at":null},
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"activate"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(body)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/currency_exchanges/activate").withHeaders(jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token")))).get
      status(resp) mustBe OK

    }

    "forward PUT /api/currency_exchanges/deactivate" in {

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/currency_exchanges/deactivate",
           |"body":{"updated_at":null},
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"deactivate"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(Json.parse(body)),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/currency_exchanges/deactivate").withHeaders(jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token")))).get
      status(resp) mustBe OK

    }

    "forward PUT /api/currency_exchanges/:id/activate" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        """
          |{
          |  "updated_at": "2019-11-20T07:59:26.370Z"
          |}
        """.stripMargin.replaceAll("\n", "")

      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/currency_exchanges/$id/activate",
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"deactivate"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(body),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(*, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/currency_exchanges/$id/activate",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get
      status(resp) mustBe OK

    }

    "forward PUT /api/currency_exchanges/:id/deactivate" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        """
          |{
          |  "updated_at": "2019-11-20T07:59:26.370Z"
          |}
        """.stripMargin.replaceAll("\n", "")

      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/currency_exchanges/$id/deactivate",
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_exchanges","action":"deactivate"}""".stripMargin.replaceAll("\n", "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "tasks",
        body = Some(body),
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(*, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(PUT, s"/api/currency_exchanges/$id/deactivate", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get
      status(resp) mustBe OK

    }
  }

}

