package tech.pegb.backoffice

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.currencyexchange.Constants
import tech.pegb.backoffice.util.Implicits._

class SpreadsIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "Get CurrencySpreads API - Positive" should {
    "return list of currency spreads when there is no filter" in {
      val resp = route(app, FakeRequest(GET, s"/api/spreads?order_by=id").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"total":4,"results":[
           |{"id":"6c5fffdd-6056-4d81-a348-34b486ce7e6a","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"international_remittance","channel":"bank","institution":null,"spread":0.250000,"updated_by":"pegbuser","updated_at":"2019-02-16T00:00:00Z"},
           |{"id":"747a1077-46ed-43a2-86b9-09817a751a44","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"currency_exchange","channel":null,"institution":null,"spread":0.200000,"updated_by":"pegbuser","updated_at":"2019-02-28T00:00:00Z"},
           |{"id":"938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0","currency_exchange_id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09","buy_currency":"EUR","sell_currency":"AED","transaction_type":"international_remittance","channel":"mobile_money","institution":"mashreq","spread":0.050000,"updated_by":"pegbuser","updated_at":"2019-02-26T00:00:00Z"},
           |{"id":"e5bb6aef-b2b3-4cbf-86ca-e3d858d6209b","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"currency_exchange","channel":null,"institution":null,"spread":0.150000,"updated_by":"pegbuser","updated_at":"2019-01-30T00:00:00Z"}],
           |"limit":null,"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expected
    }

    "return list of currency spreads filter by currency_exchange_id and ordered by created_at descending (recent first)" in {
      val resp = route(app, FakeRequest(GET, s"/api/spreads?currency_exchange_id=bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4&order_by=-created_at").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"total":3,"results":[
           |{"id":"747a1077-46ed-43a2-86b9-09817a751a44","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"currency_exchange","channel":null,"institution":null,"spread":0.200000,"updated_by":"pegbuser","updated_at":"2019-02-28T00:00:00Z"},
           |{"id":"6c5fffdd-6056-4d81-a348-34b486ce7e6a","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"international_remittance","channel":"bank","institution":null,"spread":0.250000,"updated_by":"pegbuser","updated_at":"2019-02-16T00:00:00Z"},
           |{"id":"e5bb6aef-b2b3-4cbf-86ca-e3d858d6209b","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"currency_exchange","channel":null,"institution":null,"spread":0.150000,"updated_by":"pegbuser","updated_at":"2019-01-30T00:00:00Z"}],
           |"limit":null,"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return list of currency spreads filter by currency" in {
      val resp = route(app, FakeRequest(GET, s"/api/spreads?currency=EUR").withHeaders(AuthHeader)).get
      val expected =
        s"""{
         |"total":1,
         |"results":[
         |{"id":"938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0",
         |"currency_exchange_id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
         |"buy_currency":"EUR",
         |"sell_currency":"AED",
         |"transaction_type":"international_remittance",
         |"channel":"mobile_money",
         |"institution":"mashreq",
         |"spread":0.050000,
         |"updated_by":"pegbuser",
         |"updated_at":"2019-02-26T00:00:00Z"
         |}],
         |"limit":null,
         |"offset":null
         |}""".stripMargin.
          replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of currency spreads filter by transaction type" in {
      val resp = route(app, FakeRequest(GET, s"/api/spreads?transaction_type=international_remittance&order_by=spread").withHeaders(AuthHeader)).get
      val expected = s"""{
                        |"total":2,
                        |"results":[
                        |{"id":"938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0","currency_exchange_id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09","buy_currency":"EUR","sell_currency":"AED",
                        |"transaction_type":"international_remittance","channel":"mobile_money","institution":"mashreq","spread":0.050000,"updated_by":"pegbuser","updated_at":"2019-02-26T00:00:00Z"},
                        |{"id":"6c5fffdd-6056-4d81-a348-34b486ce7e6a","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED",
                        |"transaction_type":"international_remittance","channel":"bank","institution":null,"spread":0.250000,"updated_by":"pegbuser","updated_at":"2019-02-16T00:00:00Z"}
                        |],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return currency spread by uuid" in {
      val resp = route(app, FakeRequest(GET, s"/api/spreads/938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0").withHeaders(AuthHeader)).get
      val expected =
        s"""{
          |"id":"938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0",
          |"currency_exchange_id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
          |"buy_currency":"EUR",
          |"sell_currency":"AED",
          |"transaction_type":"international_remittance",
          |"channel":"mobile_money",
          |"institution":"mashreq",
          |"spread":0.050000,
          |"updated_by":"pegbuser",
          |"updated_at":"2019-02-26T00:00:00Z"
          |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }
  }

  "Get CurrencySpreads API - Negative" should {
    "return notfoundError when spread does not exist" in {
      val requestId = UUID.randomUUID()
      val spreadId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/spreads/$spreadId")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s""""code":"NotFoundEntity","msg":"spread with id [$spreadId] not found""""

      status(resp) mustBe NOT_FOUND
      contentAsString(resp).contains(expected)
    }

    "return validationError when order_by contains invalid fields" in {

      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/spreads?order_by=deadbeef")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected =
        s"""{
           |"id":"$requestId",
           |"code":"InvalidRequest",
           |"msg":"invalid field for order_by found. Valid fields: ${Constants.validSpreadsOrderByFields.defaultMkString}"
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustEqual expected
    }

    "return validationError when partial_match contains invalid fields" in {

      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/spreads?partial_match=deadbeef")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected =
        s"""{
           |"id":"$requestId",
           |"code":"InvalidRequest",
           |"msg":"invalid field for partial matching found. Valid fields: ${Constants.validSpreadsPartialMatchFields.toSeq.sorted.defaultMkString}"
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustEqual expected
    }
  }

}
