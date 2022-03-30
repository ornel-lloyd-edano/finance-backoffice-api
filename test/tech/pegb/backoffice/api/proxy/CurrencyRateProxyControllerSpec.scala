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
import play.api.test.Helpers.{DELETE, GET, POST, PUT, route, status, _}
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, ClaimContent}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, ProxyTestHelper, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class CurrencyRateProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
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

  val Endpoint = "currency_rates"

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

    "forward PUT /api/currency_rates/:id to internal" in {
      val id = 1
      val jsonRequest =
        """
          |{
          |  "main_currency": {
          |    "id": "1L",
          |    "code": "currency code",
          |    "description": "currency desc"
          |  },
          |  "rates": [
          |    {
          |      "code": "currency code",
          |      "description": "currency desc",
          |      "buy_rate": {
          |        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          |        "rate": 0
          |      },
          |      "sell_rate": {
          |        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          |        "rate": 0
          |      }
          |    }
          |  ],
          |  "updated_at": "2019-11-21T11:52:01.965Z"
          |}
        """.stripMargin.replaceAll("\n", "").trim

      val backofficeHost = "$backoffice_api_host"
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/currency_rates/$id",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"currency_rates","action":"update"}""".stripMargin.replaceAll("\n", "")

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
      val resp = route(app, FakeRequest(PUT, s"/api/currency_rates/$id", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward GET /api/currency_rates to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"currency_rates",
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

      val resp = route(app, FakeRequest(GET, s"/api/currency_rates")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }
  }

}

