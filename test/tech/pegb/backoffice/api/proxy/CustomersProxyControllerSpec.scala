package tech.pegb.backoffice.api.proxy

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import akka.util.ByteString
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
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

//TODO our beloved Lloyd will fix it :)
@Ignore
class CustomersProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
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

  val Endpoint = "customers"
  val EndpointWithAccount = "accounts"
  val EndpointWithTransaction = "transactions"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)
  val backOfficeUserForAccount: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, EndpointWithAccount)
  val backOfficeUserForTxn: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, EndpointWithTransaction)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val claimContentWithAccount: ClaimContent = ClaimContent.from(backOfficeUserForAccount)
  val claimContentWithTxn: ClaimContent = ClaimContent.from(backOfficeUserForTxn)

  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)
  val tokenForUserToAccessCustomerAccount: String = tokenService.generateToken(backOfficeUser.userName, claimContentWithAccount)
  val tokenForUserToAccessCustomerTxn: String = tokenService.generateToken(backOfficeUser.userName, claimContentWithTxn)

  "CustomersProxy routes" should {

    val backofficeUserHeaders = List(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name))

/*****************************
      * all GET routes
      ****************************/
    "forward GET /api/customers/:id/accounts to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"customers/$id/accounts",
        body = None,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForAccount.userName)
        .returns(Future.successful(backOfficeUserForAccount.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/customers/$id/accounts")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForUserToAccessCustomerAccount")))).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "internal route response"

    }

    "forward GET /api/customers/:id/accounts/:account_id to internal" in {
      val id = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"customers/$id/accounts/$accountId",
        body = None,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForAccount.userName)
        .returns(Future.successful(backOfficeUserForAccount.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/customers/$id/accounts/$accountId")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForUserToAccessCustomerAccount")))).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "internal route response"

    }

    "forward GET /api/customers/:customer_id/accounts/:account_id/transactions to internal" in {
      val customerId = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val backofficeUserHeaders = List(
        ("X-UserName", backOfficeUserForTxn.userName),
        ("X-RoleLevel", backOfficeUserForTxn.role.level.toString),
        ("X-BusinessUnit", backOfficeUserForTxn.businessUnit.name))

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"customers/$customerId/accounts/$accountId/transactions",
        body = None,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForTxn.userName)
        .returns(Future.successful(backOfficeUserForTxn.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(app, FakeRequest(GET, s"/api/customers/$customerId/accounts/$accountId/transactions")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForUserToAccessCustomerTxn")))).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "internal route response"

    }

    "forward GET /api/customers/:customer_id/payment_options to internal" in {
      val customerId = UUID.randomUUID()

      val Endpoint = "payment_options"

      val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)
      val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
      val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

      val backofficeUserHeaders = List(
        ("X-UserName", backOfficeUser.userName),
        ("X-RoleLevel", backOfficeUser.role.level.toString),
        ("X-BusinessUnit", backOfficeUser.businessUnit.name))

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"customers/$customerId/payment_options",
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

      val resp = route(app, FakeRequest(GET, s"/api/customers/$customerId/payment_options")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "internal route response"

    }

    "forward GET /api/customers to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"customers",
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

      val resp = route(app, FakeRequest(GET, s"/api/customers")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "internal route response"
    }

    "forward GET /api/customers/:id to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"customers/$id",
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

      val resp = route(app, FakeRequest(GET, s"/api/customers/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe "internal route response"
    }

/*************************************
      * all PUT routes
      ************************************/

    "forward PUT /api/customers/:id/accounts/:accountId/activate" in {
      val id = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        """{"updated_at":"2019-12-11T13:32:21.420Z"}""".stripMargin.trim
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/customers/$id/accounts/$accountId/activate",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"accounts","action":"activate"}""".stripMargin.replaceAll(" ", "").replaceAll(System.lineSeparator(), "").trim

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(body).some,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForAccount.userName)
        .returns(Future.successful(backOfficeUserForAccount.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(
        app,
        FakeRequest(PUT, s"/api/customers/$id/accounts/$accountId/activate",
          jsonHeadersForProxyWithContentType.add(
            ("authorization", s"Bearer $tokenForUserToAccessCustomerAccount"),
            (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe "internal route response"

    }

    "forward PUT /api/customers/:id/accounts/:accountId/deactivate" in {
      val id = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        """{"updated_at":"2019-12-11T13:32:21.420Z"}""".stripMargin.trim
      val body =
        s"""
           |{"verb":"PUT",
           |"url":"$backofficeHost/customers/$id/accounts/$accountId/deactivate",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"accounts","action":"deactivate"}""".stripMargin.replaceAll(" ", "").replaceAll(System.lineSeparator(), "").trim

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(body).some,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForAccount.userName)
        .returns(Future.successful(backOfficeUserForAccount.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(
        app,
        FakeRequest(PUT, s"/api/customers/$id/accounts/$accountId/deactivate",
          jsonHeadersForProxyWithContentType.add(
            ("authorization", s"Bearer $tokenForUserToAccessCustomerAccount"),
            (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe "internal route response"

    }

    "forward DELETE /api/customers/:id/accounts/:account_id" in {
      val id = UUID.randomUUID()
      val accountId = UUID.randomUUID()

      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        """{"updated_at":"2019-12-11T13:32:21.420Z"}""".stripMargin.trim
      val body =
        s"""
           |{"verb":"DELETE",
           |"url":"$backofficeHost/customers/$id/accounts/$accountId",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"accounts","action":"delete"}""".stripMargin.replaceAll(" ", "").replaceAll(System.lineSeparator(), "").trim

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(body).some,
        queryParameters = Vector(),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)
      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForAccount.userName)
        .returns(Future.successful(backOfficeUserForAccount.asRight[ServiceError]))

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      val resp = route(
        app,
        FakeRequest(DELETE, s"/api/customers/$id/accounts/$accountId",
          jsonHeadersForProxyWithContentType.add(
            ("authorization", s"Bearer $tokenForUserToAccessCustomerAccount"),
            (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe "internal route response"

    }
  }
}
