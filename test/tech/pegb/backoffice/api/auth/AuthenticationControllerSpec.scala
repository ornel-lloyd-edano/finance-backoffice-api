package tech.pegb.backoffice.api.auth

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.{AuthenticationService, PasswordService, TokenService}
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.LoginUsername
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.Constants._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticationControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {
  implicit val ec: ExecutionContext = TestExecutionContext.genericOperations
  implicit val tokenExpirationInMinutes = TokenExpiration(30)

  val authenticationService: AuthenticationService = stub[AuthenticationService]
  val latestVersionService: LatestVersionService = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[AuthenticationService].to(authenticationService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)
  private val requestDateFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  val passwordService: PasswordService = inject[PasswordService]
  val tokenService: TokenService = inject[TokenService]

  "Authentication routes" should {

    val password = "P@ssw0rd123!"
    val hashedPassword = passwordService.hashPassword(password)

    val bu = BackOfficeUser(
      id = UUID.randomUUID(),
      userName = "pumkinfreak",
      hashedPassword = hashedPassword.some,
      role = Role(
        id = UUID.randomUUID(),
        name = "Manager",
        level = 1,
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some),
      businessUnit = BusinessUnit(
        id = UUID.randomUUID(),
        name = "Finance",
        createdBy = "pegbuser",
        createdAt = now,
        updatedBy = "pegbuser".some,
        updatedAt = now.some),
      permissions = Nil,
      email = Email("l.ward@gmail.com"),
      phoneNumber = None,
      firstName = "L",
      middleName = "W".some,
      lastName = "Salgado",
      description = None,
      homePage = None,
      activeLanguage = None,
      customData = None,
      lastLoginTimestamp = None,
      createdBy = "pegbuser",
      createdAt = now,
      updatedBy = "pegbuser".some,
      updatedAt = now.some)

    "provide login config (e.g. if captcha is enabled)" in {
      val expectedResult =
        s"""{"require_captcha":true}""".stripMargin.replace(System.lineSeparator(), "")

      (authenticationService.status _)
        .when()
        .returns(Future.successful(LoginStatusResponse(true).asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/status")).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResult

    }

    "fail to login with empty user name" in {
      val body =
        s"""
           |{
           |"user":"",
           |"password":"",
           |"captcha":"dghwefkslm"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"/api/login", jsonHeaders, body)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId","code":"InvalidRequest","msg":"assertion failed: empty LoginUsername"}
           |""".stripMargin.stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson

    }

    "fail to login with empty password" in {
      val body =
        s"""
           |{
           |"user":"pumkinfreak",
           |"password":"",
           |"captcha":"dghwefkslm"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"/api/login", jsonHeaders, body)).get
      val expectedJson =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Password cannot be null or empty"}
           |""".stripMargin.stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }

    "validate received token" in {
      val claim = ClaimContent.from(bu)
      val token = tokenService.generateToken("pumkinfreak", claim)
      val expectedResult =
        s"""{
           |"token":"$token",
           |"user":{
           |"id":"${bu.id}",
           |"user_name":"pumkinfreak",
           |"email":"l.ward@gmail.com",
           |"phone_number":null,
           |"first_name":"L",
           |"middle_name":"W",
           |"last_name":"Salgado",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
           |"role":{
           |"id":"${bu.role.id}",
           |"name":"Manager",
           |"level":1,
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |"business_unit":{
           |"id":"${bu.businessUnit.id}",
           |"name":"Finance",
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |"permissions":[],
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}}}""".stripMargin.replace(System.lineSeparator(), "")

      (authenticationService.validateToken _)
        .when(token)
        .returns(Future.successful((bu, token).asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/api/validate_token")
        .withHeaders(jsonHeaders.add(("authorization", s"Bearer $token")))).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResult
    }

    "login with valid credentials" in {
      val claim = ClaimContent.from(bu)
      val token = tokenService.generateToken("pumkinfreak", claim)

      val body =
        s"""{
           |"user":"pumkinfreak",
           |"password":"$password",
           |"captcha":"someCaptcha"
           |}""".stripMargin

      (authenticationService.login _)
        .when(LoginUsername("pumkinfreak"), password, "someCaptcha".some)
        .returns(Future.successful((bu, token).asRight[ServiceError]))

      val resp = route(app, FakeRequest(POST, s"/api/login", jsonHeaders, body)).get

      val expectedJson =
        s"""
           |{
           |"token":"$token",
           |"user":{
           |"id":"${bu.id}",
           |"user_name":"pumkinfreak",
           |"email":"l.ward@gmail.com",
           |"phone_number":null,
           |"first_name":"L",
           |"middle_name":"W",
           |"last_name":"Salgado",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
           |"role":{
           |"id":"${bu.role.id}",
           |"name":"Manager",
           |"level":1,
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |"business_unit":{
           |"id":"${bu.businessUnit.id}",
           |"name":"Finance",
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |"permissions":[],
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}}}""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "update password returns login response" in {
      val claim = ClaimContent.from(bu)
      val token = tokenService.generateToken("pumkinfreak", claim)

      val body =
        s"""{
           |"user":"pumkinfreak",
           |"old_password":"$password",
           |"new_password":"new_password"
           |}""".stripMargin

      (authenticationService.updatePassword _)
        .when(LoginUsername("pumkinfreak"), password, "new_password", mockRequestDate.toLocalDateTimeUTC)
        .returns(Future.successful((bu, token).asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/update_password", jsonHeaders, body)).get
      val expectedJson =
        s"""
           |{
           |"token":"$token",
           |"user":{
           |"id":"${bu.id}",
           |"user_name":"pumkinfreak",
           |"email":"l.ward@gmail.com",
           |"phone_number":null,
           |"first_name":"L",
           |"middle_name":"W",
           |"last_name":"Salgado",
           |"description":null,
           |"home_page":null,
           |"active_language":null,
           |"last_login_timestamp":null,
           |"custom_data":null,
           |"role":{
           |"id":"${bu.role.id}",
           |"name":"Manager",
           |"level":1,
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |"business_unit":{
           |"id":"${bu.businessUnit.id}",
           |"name":"Finance",
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |"permissions":[],
           |"created_by":"pegbuser",
           |"updated_by":"pegbuser",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}}}""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }

    "reset password" in {
      val body =
        s"""{
           |"user_name":"pumkinfreak",
           |"email":"l.ward@gmail.com",
           |"captcha":"someCaptcha"
           |}
         """.stripMargin

      (authenticationService.sendPasswordResetLink _)
        .when(LoginUsername("pumkinfreak"), Email("l.ward@gmail.com"), None, "someCaptcha".some)
        .returns(Future.successful(UnitInstance.toRight))

      val resp = route(app, FakeRequest(POST, s"/api/reset_password",
        jsonHeaders.add(("REFERER", "http//whizmo.app")), body)).get

      status(resp) mustBe OK
    }

    "reset password invalid email" in {
      val body =
        s"""{
           |"user_name":"pumkinfreak",
           |"email":"l.ward",
           |"captcha":"someCaptcha"
           |}
         """.stripMargin

      val resp = route(app, FakeRequest(POST, s"/api/reset_password",
        jsonHeaders.add(("REFERER", "http//whizmo.app")), body)).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe
        s"""
           |{
           |"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"assertion failed: invalid email"}""".stripMargin.replace(System.lineSeparator(), "")
    }

    "reset password get" in {
      val claim = ClaimContent.from(bu)
      val token = tokenService.generateToken("pumkinfreak", claim)

      (authenticationService.validatePasswordResetToken _)
        .when(token)
        .returns(Future.successful(UnitInstance.toRight))

      val resp = route(app, FakeRequest(GET, s"/api/reset_password?token=$token")).get

      status(resp) mustBe OK
    }

    "reset password update" in {
      val claim = ClaimContent.from(bu)
      val token = tokenService.generateToken("pumkinfreak", claim)

      val body =
        s"""{
           |"password":"NewP@ssword123!",
           |"token":"$token"
           |}
         """.stripMargin //PasswordReset

      (authenticationService.resetPassword _)
        .when("NewP@ssword123!", token, mockRequestDate.toLocalDateTimeUTC)
        .returns(Future.successful((bu, token).asRight[ServiceError]))

      val resp = route(app, FakeRequest(PUT, s"/api/reset_password", jsonHeaders, body)).get

      status(resp) mustBe OK
    }

  }
}
