package tech.pegb.backoffice.api.proxy

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.UUID

import cats.implicits._
import akka.util.ByteString
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HttpEntity
import play.api.inject.{Binding, bind}
import play.api.mvc.{Request, ResponseHeader, Result}
import play.api.test.FakeRequest
import play.api.http.HttpEntity
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, route, status, _}
import play.api.test.Helpers.{GET, PUT, route, status}
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, ClaimContent}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, ProxyTestHelper, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class BackOfficeUserProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
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

  val Endpoint = "back_office_users"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, Endpoint)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

  "BackOfficeUserProxy routes" should {

    val backofficeUserHeaders = List(
      ("X-UserName", backOfficeUser.userName),
      ("X-RoleLevel", backOfficeUser.role.level.toString),
      ("X-BusinessUnit", backOfficeUser.businessUnit.name))

    "forward GET /api/back_office_users to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"back_office_users",
        body = None,
        queryParameters = Vector(("user_name", "ujali"), ("first_name", "rocks")),
        headers = jsonHeadersForProxy.add(backofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, backofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUser.userName)
        .returns(Future.successful(backOfficeUser.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/back_office_users?user_name=ujali&first_name=rocks")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/back_office_users/:id to internal" in {
      val id = UUID.randomUUID()
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"back_office_users/$id",
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

      val resp = route(app, FakeRequest(GET, s"/api/back_office_users/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK

    }

    "forward PUT /api/back_office_users/:id to internal" in {
      val id = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{
           | "updated_at": "2019-11-20T06:03:24.609Z"
           |}
         """.stripMargin.replaceAll("\n", "")
      val proxyRequest = ProxyRequest(
        httpMethod = "PUT",
        url = s"back_office_users/$id",
        body = Some(Json.parse(jsonRequest.trim)),
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

      val resp = route(app, FakeRequest(PUT, s"/api/back_office_users/$id", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK
    }

    "forward DELETE /api/back_office_users/:id to internal" in {
      val id = UUID.randomUUID()

      val jsonRequest =
        s"""
           |{
           | "updated_at": "2019-11-20T06:03:24.609Z"
           |}
         """.stripMargin.replaceAll("\n", "")
      val proxyRequest = ProxyRequest(
        httpMethod = "DELETE",
        url = s"back_office_users/$id",
        body = Some(jsonRequest),
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

      val resp = route(app, FakeRequest(DELETE, s"/api/back_office_users/$id", jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK
    }

  }

}
