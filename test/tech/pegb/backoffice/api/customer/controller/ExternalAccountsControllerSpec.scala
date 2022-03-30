package tech.pegb.backoffice.api.customer.controller

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.customer.controllers.ExternalAccountsController
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class ExternalAccountsControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val endpoint = inject[ExternalAccountsController].getRoute

  "ExternalAccountsController" should {
    "respond 200 OK with external account json in the body from GET /external_accounts/:id" ignore {

      val externalAccId = UUID.randomUUID()

      val fakeRequest = FakeRequest(GET, s"/external_accounts/$externalAccId")
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 404 NotFound with external account not found api error json in the body from GET /external_accounts/:id" ignore {
      val externalAccId = UUID.randomUUID()

      val fakeRequest = FakeRequest(GET, s"/external_accounts/$externalAccId")
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp) mustBe expected
    }

    "respond 200 OK with paginated external account json in the body from GET /external_accounts" ignore {
      val userId = UUID.randomUUID()

      val fakeRequest = FakeRequest(GET, s"/external_accounts")
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 201 Created with external account json in the body from POST /external_accounts" ignore {

      val jsonRequest =
        s"""
           |
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/external_accounts", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expected
    }

    "respond 400 BadRequest with validation api error json in the body from POST /external_accounts" ignore {
      val userId = UUID.randomUUID()

      val jsonRequest =
        s"""
           |
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/external_accounts", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "respond 200 OK with external account json in the body from PUT /external_accounts/:id" ignore {
      val externalAccId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |
         """.stripMargin

      val fakeRequest = FakeRequest(PUT, s"/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "respond 400 BadRequest with validation api error json in the body from PUT /external_accounts/:id" ignore {
      val externalAccId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |
         """.stripMargin

      val fakeRequest = FakeRequest(PUT, s"/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "respond 204 No Content from DELETE /external_accounts/:id" ignore {
      val externalAccId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |
         """.stripMargin

      val fakeRequest = FakeRequest(DELETE, s"/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NO_CONTENT
    }

    "respond 204 No Content from DELETE /external_accounts/:id even if external account is not found" ignore {
      val externalAccId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |
         """.stripMargin

      val fakeRequest = FakeRequest(DELETE, s"/external_accounts/$externalAccId", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get
      val expected =
        s"""
           |
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NO_CONTENT
    }

  }

}
