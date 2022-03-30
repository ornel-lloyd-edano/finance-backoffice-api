package tech.pegb.backoffice

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class CreateSpreadIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }
  
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val usdFxUUID = "bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4"
  val euroFxUUID = "b566429b-e166-4cf6-83cb-7d8cf5ec9f09"
  val cnyFxUUID = "3a01ea86-de7b-414d-8f8a-757f101ccd13"

  "CreateSpreads via CurrencyExchange API - Positive" should {

    "return newly created spread: international_remittance" in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "international_remittance",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$usdFxUUID/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsJson(resp)

      (jsonResponse \ "currency_exchange_id").get mustBe JsString(usdFxUUID)
      (jsonResponse \ "sell_currency").get mustBe JsString("AED")
      (jsonResponse \ "transaction_type").get mustBe JsString("international_remittance")
      (jsonResponse \ "channel").get mustBe JsString("bank")
      (jsonResponse \ "institution").get mustBe JsString("Mashreq")
      (jsonResponse \ "spread").get mustBe JsNumber(0.010000)
      (jsonResponse \ "updated_by").get mustBe JsString("superuser")
    }

    "return newly created spread: currency_exchange" in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$euroFxUUID/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsJson(resp)

      (jsonResponse \ "currency_exchange_id").get mustBe JsString(euroFxUUID)
      (jsonResponse \ "sell_currency").get mustBe JsString("AED")
      (jsonResponse \ "transaction_type").get mustBe JsString("currency_exchange")
      (jsonResponse \ "channel").get mustBe JsNull
      (jsonResponse \ "institution").get mustBe JsNull
      (jsonResponse \ "spread").get mustBe JsNumber(0.010000)
      (jsonResponse \ "updated_by").get mustBe JsString("superuser")
    }
  }

  "CreateSpreads API via CurrencyExchange API - Negative" should {
    "return error on duplicate spread" in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$usdFxUUID/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"Conflict",
           |"msg":"Spread: (transaction_type: currency_exchange, channel: None, institution: None) already exists for currency_exchange: $usdFxUUID",
           |"tracking_id":"random UUID internally generated to be removed before assertion"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CONFLICT
      contentAsJson(resp).as[JsObject].keys.contains("tracking_id") mustBe true
      val respWithoutTrackingId = contentAsJson(resp).as[JsObject].-("tracking_id").toString()
      val expectedRespWithoutTrackingId = Json.parse(expectedJson).as[JsObject].-("tracking_id").toString()

      respWithoutTrackingId mustBe expectedRespWithoutTrackingId
    }
    "respond with BAD_REQUEST when transaction_type is invalid" in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "invalid_txnType",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
   "respond with BAD_REQUEST when transaction_type is international_remittance and channel is empty string " in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "international_remittance",
           |  "channel": "",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when spread > 1 " in {
      val jsonRequest =
        s"""{
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 1.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$fxId/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with NOT_FOUND when currency_exchange for spread is not found" in {
      val fakeUUID = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "transaction_type": "international_remittance",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/currency_exchanges/$fakeUUID/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expected =
        s"""{"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"Currency exchange $fakeUUID was not found"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe NOT_FOUND
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "code").get mustBe JsString("NotFound")
      (jsonResponse \ "msg").get mustBe JsString(s"Currency exchange $fakeUUID was not found")

    }
  }

  "CreateSpreads via Spread API - Positive" should {

    "return newly created spread: international_remittance" in {
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$usdFxUUID",
           |  "transaction_type": "international_remittance",
           |  "channel": "bank",
           |  "institution": "NBD",
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsJson(resp)

      (jsonResponse \ "currency_exchange_id").get mustBe JsString(usdFxUUID)
      (jsonResponse \ "sell_currency").get mustBe JsString("AED")
      (jsonResponse \ "transaction_type").get mustBe JsString("international_remittance")
      (jsonResponse \ "channel").get mustBe JsString("bank")
      (jsonResponse \ "institution").get mustBe JsString("NBD")
      (jsonResponse \ "spread").get mustBe JsNumber(0.0100)
      (jsonResponse \ "updated_by").get mustBe JsString("superuser")
    }

    "return newly created spread: currency_exchange" in {
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$cnyFxUUID",
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsJson(resp)

      (jsonResponse \ "currency_exchange_id").get mustBe JsString(cnyFxUUID)
      (jsonResponse \ "sell_currency").get mustBe JsString("AED")
      (jsonResponse \ "transaction_type").get mustBe JsString("currency_exchange")
      (jsonResponse \ "channel").get mustBe JsNull
      (jsonResponse \ "institution").get mustBe JsNull
      (jsonResponse \ "spread").get mustBe JsNumber(0.0100)
      (jsonResponse \ "updated_by").get mustBe JsString("superuser")
    }
  }

  "CreateSpreads API via Spread API - Negative" should {
    "respond with BAD_REQUEST when transaction_type is invalid" in {
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$usdFxUUID",
           |  "transaction_type": "invalid_txnType",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fxId = UUID.randomUUID()

      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when transaction_type is international_remittance and channel is empty string " in {
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$usdFxUUID",
           |  "transaction_type": "international_remittance",
           |  "channel": "",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin


      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with BAD_REQUEST when spread > 1 " in {
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$usdFxUUID",
           |  "transaction_type": "currency_exchange",
           |  "channel": null,
           |  "institution": null,
           |  "spread": 1.01
           |}""".stripMargin


      val expectedJson =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expectedJson
    }
    "respond with NOT_FOUND when currency_exchange for spread is not found" in {
      val fakeUUID = UUID.randomUUID()
      val jsonRequest =
        s"""{
           |  "currency_exchange_id": "$fakeUUID",
           |  "transaction_type": "international_remittance",
           |  "channel": "bank",
           |  "institution": "Mashreq",
           |  "spread": 0.01
           |}""".stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/spreads",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get


      status(resp) mustBe NOT_FOUND
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "code").get mustBe JsString("NotFound")
      (jsonResponse \ "msg").get mustBe JsString(s"Currency exchange $fakeUUID was not found")
    }
  }
}
