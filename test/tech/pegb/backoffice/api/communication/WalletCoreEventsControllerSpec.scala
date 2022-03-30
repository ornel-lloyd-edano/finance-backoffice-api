package tech.pegb.backoffice.api.communication

import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.{POST, route, status}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.inject.bind
import tech.pegb.backoffice.api.communication.dto.EventTypes
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future
import play.api.libs.json._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement

class WalletCoreEventsControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  val walletApplicMgmt = stub[WalletApplicationManagement]
  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[WalletApplicationManagement].to(walletApplicMgmt),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "WalletCoreEventsController POST /api/v1/core/events" should {

    "receive this payload structure {type:String,payload:{key1:value1...keyN:valueN}} and respond with NoContent if success" in {

      val mockApplicationId = 1

      val expectedResult = Future.successful(Right(Seq(UUID.randomUUID())))

      (walletApplicMgmt.persistApprovedFilesByInternalApplicationId _)
        .when(mockApplicationId).returns(expectedResult)

      val jsonRequest: JsValue = Json.parse(
        s"""
          |{
          |"type": "application_approved",
          |"payload": {
          |   "id": ${mockApplicationId}
          |}
          |}
        """.stripMargin)

      val resp = route(
        app,
        FakeRequest(POST, s"/api/v1/core/events"), jsonRequest).get

      status(resp) mustBe NO_CONTENT
    }

    "respond with InternalServerError if failed" in {

      val mockApplicationId = 1

      val expectedError = Future.successful(Left(ServiceError.unknownError("some error from couchbase or hdfs", UUID.randomUUID().toOption)))

      (walletApplicMgmt.persistApprovedFilesByInternalApplicationId _)
        .when(mockApplicationId).returns(expectedError)

      val jsonRequest: JsValue = Json.parse(
        s"""
           |{
           |"type": "application_approved",
           |"payload": {
           |   "id": ${mockApplicationId}
           |}
           |}
        """.stripMargin)

      val resp = route(
        app,
        FakeRequest(POST, s"/api/v1/core/events"), jsonRequest).get

      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentAsString(resp) mustBe "some error from couchbase or hdfs"
    }

    "respond with BadRequest if payload is not correct CoreEvents format" in {

      val jsonRequest: JsValue = Json.parse(
        s"""
           |{
           |"some_field": "application_approved",
           |"some_other_field": {
           |   "id": 1
           |}
           |}
        """.stripMargin)

      val resp = route(
        app,
        FakeRequest(POST, s"/api/v1/core/events"), jsonRequest).get

      status(resp) mustBe BAD_REQUEST
    }

    "respond with BadRequest if application_id in payload is not integer" in {

      val mockApplicationId = "nakenfab378ryhjfa"

      val jsonRequest: JsValue = Json.parse(
        s"""
           |{
           |"type": "application_approved",
           |"payload": {
           |   "id": "${mockApplicationId}"
           |}
           |}
        """.stripMargin)

      val resp = route(
        app,
        FakeRequest(POST, s"/api/v1/core/events"), jsonRequest).get

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp) mustBe "id expected should be integer"
    }

    "respond with BadRequest if type is 'application_approved' but payload does not contain application_id" in {

      val jsonRequest: JsValue = Json.parse(
        s"""
           |{
           |"type": "${EventTypes.ApplicationApproved}",
           |"payload": {
           |}
           |}
        """.stripMargin)

      val resp = route(
        app,
        FakeRequest(POST, s"/api/v1/core/events"), jsonRequest).get

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp) mustBe s"id expected for event type [${EventTypes.ApplicationApproved}]"
    }

    "respond with BadRequest if event type is not recognized" in {

      val mockApplicationId = 1

      val jsonRequest: JsValue = Json.parse(
        s"""
           |{
           |"type": "Christmas",
           |"payload": {
           |   "id": ${mockApplicationId}
           |}
           |}
        """.stripMargin)

      val resp = route(
        app,
        FakeRequest(POST, s"/api/v1/core/events"), jsonRequest).get

      status(resp) mustBe BAD_REQUEST

      contentAsString(resp) mustBe "event type [Christmas] is not recognized"
    }
  }

}
