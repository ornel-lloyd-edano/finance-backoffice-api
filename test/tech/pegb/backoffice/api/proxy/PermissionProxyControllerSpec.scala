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

class PermissionProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
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

  val EndPoint = "permissions"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)
  val backOfficeUser: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, EndPoint)

  val claimContent: ClaimContent = ClaimContent.from(backOfficeUser)
  val token: String = tokenService.generateToken(backOfficeUser.userName, claimContent)

  val backofficeUserHeaders = List(
    ("X-UserName", backOfficeUser.userName),
    ("X-RoleLevel", backOfficeUser.role.level.toString),
    ("X-BusinessUnit", backOfficeUser.businessUnit.name))

  "PermissionProxy routes" should {

    "forward POST /api/permissions to internal" in {
      val jsonRequest =
        s"""{
           |"scope_id":"ed4be9e3-bf3c-4268-aef9-b8c843babd4c",
           |"permission_key":{
           |"role_id":"5c095b24-c695-45a8-9bac-abef6366acda",
           |"bu_id":"931f11af-9aca-417d-b910-0557f1f3ac00"}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = "permissions",
        body = jsonRequest.some,
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

      val resp = route(app, FakeRequest(POST, s"/api/permissions",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward GET /api/permissions/:id to internal" in {
      val id = UUID.randomUUID()

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"permissions/$id",
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

      val resp = route(app, FakeRequest(GET, s"/api/permissions/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }

    "forward GET /api/permissions to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"permissions",
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

      val resp = route(app, FakeRequest(GET, s"/api/permissions")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
    }

    "forward PUT /api/permissions/:id to internal" in {
      val id = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |"scope_id":"ed4be9e3-bf3c-4268-aef9-b8c843babd4c",
           |"permission_key":{
           |"role_id":"5c095b24-c695-45a8-9bac-abef6366acda",
           |"bu_id":"931f11af-9aca-417d-b910-0557f1f3ac00"},
           |"updated_at": "2019-12-10T13:01:29.717Z"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val proxyRequest = ProxyRequest(
        httpMethod = "PUT",
        url = s"permissions/$id",
        body = jsonRequest.some,
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

      val resp = route(app, FakeRequest(PUT, s"/api/permissions/$id",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward DELETE /api/permissions/:id to internal" in {
      val id = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "updated_at": "2019-12-10T13:04:41.880Z"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val proxyRequest = ProxyRequest(
        httpMethod = "PUT",
        url = s"permissions/$id",
        body = jsonRequest.some,
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

      val resp = route(app, FakeRequest(DELETE, s"/api/permissions/$id",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $token"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }
  }
}
