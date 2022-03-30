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

class NotificationMgmtProxyControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with ProxyTestHelper {
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

  val NotificationEndPoint = "notifications"
  val NotificationTemplateEndPoint = "notification_templates"

  val password = "P@ssw0rd123!"
  val hashedPassword: String = passwordService.hashPassword(password)

  val backOfficeUserForNotification: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, NotificationEndPoint)
  val backOfficeUserForNotificationTemp: BackOfficeUser = createTestBackOfficeUser(hashedPassword.some, NotificationTemplateEndPoint)

  val claimContentForNotification: ClaimContent = ClaimContent.from(backOfficeUserForNotification)
  val claimContentForNotificationTemplate: ClaimContent = ClaimContent.from(backOfficeUserForNotificationTemp)

  val tokenForNotification: String = tokenService.generateToken(backOfficeUserForNotification.userName, claimContentForNotification)
  val tokenForNotificationTemplate: String = tokenService.generateToken(backOfficeUserForNotification.userName, claimContentForNotificationTemplate)

  "NotificationMgmtProxy routes" should {

    val notificationBackofficeUserHeaders = List(
      ("X-UserName", backOfficeUserForNotification.userName),
      ("X-RoleLevel", backOfficeUserForNotification.role.level.toString),
      ("X-BusinessUnit", backOfficeUserForNotification.businessUnit.name))

    val notificationTemplateBackofficeUserHeaders = List(
      ("X-UserName", backOfficeUserForNotification.userName),
      ("X-RoleLevel", backOfficeUserForNotification.role.level.toString),
      ("X-BusinessUnit", backOfficeUserForNotification.businessUnit.name))

    "forward POST /api/notification_templates to internal" in {
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        s"""{
           |  "name": "template_1",
           |  "default_title": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "default_content": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "channels": ["sms", "push"],
           |  "description": "description of template 1"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val taskBody =
        s"""
           |{
           |"verb":"POST",
           |"url":"$backofficeHost/notification_templates",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"notification_templates",
           |"action":"create"
           |}""".stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(taskBody).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationTemplateBackofficeUserHeaders: _*).toSimpleMap.toSet)
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationTemplateBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotificationTemp.userName)
        .returns(Future.successful(backOfficeUserForNotificationTemp.asRight[ServiceError]))

      val resp = route(app, FakeRequest(POST, s"/api/notification_templates",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $tokenForNotificationTemplate"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward PUT /api/notification_templates/:id/deactivate to internal" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest = s"""{"updated_at": "2019-12-10T11:28:02.377Z"}"""

      val taskBody =
        s"""
           |{
           |"verb":"PUT",
           |"url":"$backofficeHost/notification_templates/$id/deactivate",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"notification_templates",
           |"action":"deactivate"
           |}""".stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(taskBody).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationTemplateBackofficeUserHeaders: _*).toSimpleMap.toSet)
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationTemplateBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotificationTemp.userName)
        .returns(Future.successful(backOfficeUserForNotificationTemp.asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/notification_templates/$id/deactivate",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $tokenForNotificationTemplate"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward PUT /api/notification_templates/:id/activate to internal" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest = s"""{"updated_at": "2019-12-10T11:28:02.377Z"}"""

      val taskBody =
        s"""
           |{
           |"verb":"PUT",
           |"url":"$backofficeHost/notification_templates/$id/activate",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"notification_templates",
           |"action":"activate"
           |}""".stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(taskBody).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationTemplateBackofficeUserHeaders: _*).toSimpleMap.toSet)
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationTemplateBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotificationTemp.userName)
        .returns(Future.successful(backOfficeUserForNotificationTemp.asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/notification_templates/$id/activate",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $tokenForNotificationTemplate"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward PUT /api/notification_templates/:id to internal" in {
      val id = UUID.randomUUID()
      val backofficeHost = "$backoffice_api_host"
      val jsonRequest =
        s"""{
           |"title_resource":"some title resource value",
           |"content_resource":"ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |"updated_at": "2019-12-10T11:48:59.846Z"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val taskBody =
        s"""
           |{
           |"verb":"PUT",
           |"url":"$backofficeHost/notification_templates/$id",
           |"body":$jsonRequest,
           |"headers":{"request-id":"${mockRequestId.toString}",
           |"X-UserName":"scala.user"},
           |"module":"notification_templates",
           |"action":"update"
           |}""".stripMargin

      val proxyRequest = ProxyRequest(
        httpMethod = "POST",
        url = s"tasks",
        body = Json.parse(taskBody).some,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationTemplateBackofficeUserHeaders: _*).toSimpleMap.toSet)
      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationTemplateBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotificationTemp.userName)
        .returns(Future.successful(backOfficeUserForNotificationTemp.asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/notification_templates/$id",
        jsonHeadersForProxyWithContentType.add(("authorization", s"Bearer $tokenForNotificationTemplate"), (strictDeserializationKey, "false")), jsonRequest)).get

      status(resp) mustBe OK

    }

    "forward GET /api/notification_templates to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"notification_templates",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationTemplateBackofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationTemplateBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotificationTemp.userName)
        .returns(Future.successful(backOfficeUserForNotificationTemp.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/notification_templates")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForNotificationTemplate")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/notifications to internal" in {

      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"notifications",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationBackofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotification.userName)
        .returns(Future.successful(backOfficeUserForNotification.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/notifications")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForNotification")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/notification_templates/:id to internal" in {
      val id = UUID.randomUUID()
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"notification_templates/$id",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationTemplateBackofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationTemplateBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotificationTemp.userName)
        .returns(Future.successful(backOfficeUserForNotificationTemp.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/notification_templates/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForNotificationTemplate")))).get

      status(resp) mustBe OK

    }

    "forward GET /api/notifications/:id to internal" in {
      val id = UUID.randomUUID()
      val proxyRequest = ProxyRequest(
        httpMethod = "GET",
        url = s"notifications/$id",
        body = None,
        queryParameters = Vector.empty,
        headers = jsonHeadersForProxy.add(notificationBackofficeUserHeaders: _*).toSimpleMap.toSet)

      (proxyResponseHandler.handleApiProxyResponse(_: ProxyRequest[_], _: Seq[(String, String)], _: String)(_: Request[_], _: UUID))
        .when(proxyRequest, *, *, *, *)
        .returns(Future.successful(Result(
          ResponseHeader(200, notificationBackofficeUserHeaders.toMap),
          HttpEntity.Strict(ByteString("internal route response"), Some("application/json"))).asRight[ApiError]))

      (backOfficeUserService.getBackOfficeUserByUsername _)
        .when(backOfficeUserForNotification.userName)
        .returns(Future.successful(backOfficeUserForNotification.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/notifications/$id")
        .withHeaders(jsonHeadersForProxy.add(("authorization", s"Bearer $tokenForNotification")))).get

      status(resp) mustBe OK

    }
  }
}
