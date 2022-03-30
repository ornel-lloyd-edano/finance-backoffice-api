package tech.pegb.backoffice

import java.time.LocalDateTime
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.DBApi
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import play.api.test.Helpers._

class GetCurrencyExchangeIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  "Get CurrencyExchange API - Positive" should {

    "return list of currency exchange when there is no filter" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?order_by=currency_code,-base_currency").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"total":6,
           |"results":[
           |{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"sell_currency":"USD",
           |"buy_currency":"AED",
           |"currency_description":"United Arab Emirates Dirham",
           |"rate":0.010152,
           |"provider":"Currency Cloud",
           |"balance":5000000.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"sell_currency":"EUR",
           |"buy_currency":"AED",
           |"currency_description":"United Arab Emirates Dirham",
           |"rate":0.009062,"provider":"Currency Cloud",
           |"balance":5000000.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"51cd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"sell_currency":"AED",
           |"buy_currency":"CHF",
           |"currency_description":"Swiss Franc",
           |"rate":152.201400,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"inactive",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"3a01ea86-de7b-414d-8f8a-757f101ccd13",
           |"sell_currency":"AED",
           |"buy_currency":"CNY",
           |"currency_description":"Chinese Yuan",
           |"rate":75.950100,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"sell_currency":"AED",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102000,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"sell_currency":"AED",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.980000,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return list of currency exchange filter by status" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?status=active&order_by=currency_code,-base_currency").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"total":5,
           |"results":[
           |{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"sell_currency":"USD",
           |"buy_currency":"AED",
           |"currency_description":"United Arab Emirates Dirham",
           |"rate":0.010152,
           |"provider":"Currency Cloud",
           |"balance":5000000.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"sell_currency":"EUR",
           |"buy_currency":"AED",
           |"currency_description":"United Arab Emirates Dirham",
           |"rate":0.009062,"provider":"Currency Cloud",
           |"balance":5000000.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"3a01ea86-de7b-414d-8f8a-757f101ccd13",
           |"sell_currency":"AED",
           |"buy_currency":"CNY",
           |"currency_description":"Chinese Yuan",
           |"rate":75.950100,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"sell_currency":"AED",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102000,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"sell_currency":"AED",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.980000,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return list of currency exchange filter by base_currency" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?base_currency=AED&order_by=currency_code").withHeaders(AuthHeader)).get

      val expected = s"""{
         |"total":4,
         |"results":[
         |{
         |"id":"51cd74ee-3a94-45b4-aac2-85856aaffd3f",
         |"sell_currency":"AED",
         |"buy_currency":"CHF",
         |"currency_description":"Swiss Franc",
         |"rate":152.201400,
         |"provider":"Ebury",
         |"balance":0.00,
         |"status":"inactive",
         |"updated_at":"2019-02-25T00:00:00Z"
         |},
         |{
         |"id":"3a01ea86-de7b-414d-8f8a-757f101ccd13",
         |"sell_currency":"AED",
         |"buy_currency":"CNY",
         |"currency_description":"Chinese Yuan",
         |"rate":75.950100,
         |"provider":"Ebury",
         |"balance":0.00,
         |"status":"active",
         |"updated_at":"2019-02-25T00:00:00Z"
         |},
         |{
         |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
         |"sell_currency":"AED",
         |"buy_currency":"EUR",
         |"currency_description":"Euro",
         |"rate":112.102000,
         |"provider":"Currency Cloud",
         |"balance":10519.00,
         |"status":"active",
         |"updated_at":"2019-02-25T00:00:00Z"
         |},
         |{
         |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
         |"sell_currency":"AED",
         |"buy_currency":"USD",
         |"currency_description":"US Dollar",
         |"rate":99.980000,
         |"provider":"Currency Cloud",
         |"balance":17526.50,
         |"status":"active",
         |"updated_at":"2019-02-25T00:00:00Z"
         |}],
         |"limit":null,
         |"offset":null
         |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return list of currency exchange filter by currency_code" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?currency_code=USD&order_by=currency_code").withHeaders(AuthHeader)).get

      val expected = s"""{
                        |"total":1,
                        |"results":[
                        |{
                        |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
                        |"sell_currency":"AED",
                        |"buy_currency":"USD",
                        |"currency_description":"US Dollar",
                        |"rate":99.980000,
                        |"provider":"Currency Cloud",
                        |"balance":17526.50,
                        |"status":"active",
                        |"updated_at":"2019-02-25T00:00:00Z"
                        |}],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return list of currency exchange filter by like provider" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?provider=Cloud&partial_match=provider&order_by=balance,-base_currency").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"total":4,
           |"results":[
           |{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"sell_currency":"AED",
           |"buy_currency":"EUR",
           |"currency_description":"Euro",
           |"rate":112.102000,
           |"provider":"Currency Cloud",
           |"balance":10519.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"sell_currency":"AED",
           |"buy_currency":"USD",
           |"currency_description":"US Dollar",
           |"rate":99.980000,
           |"provider":"Currency Cloud",
           |"balance":17526.50,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"sell_currency":"USD",
           |"buy_currency":"AED",
           |"currency_description":"United Arab Emirates Dirham",
           |"rate":0.010152,
           |"provider":"Currency Cloud",
           |"balance":5000000.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |},
           |{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"sell_currency":"EUR",
           |"buy_currency":"AED",
           |"currency_description":"United Arab Emirates Dirham",
           |"rate":0.009062,"provider":"Currency Cloud",
           |"balance":5000000.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "return list of currency exchange with multiple filters" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?provider=Ebury&currency_code=CNY&partial_match=currency_code&order_by=-rate").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"total":1,
           |"results":[
           |{
           |"id":"3a01ea86-de7b-414d-8f8a-757f101ccd13",
           |"sell_currency":"AED",
           |"buy_currency":"CNY",
           |"currency_description":"Chinese Yuan",
           |"rate":75.950100,
           |"provider":"Ebury",
           |"balance":0.00,
           |"status":"active",
           |"updated_at":"2019-02-25T00:00:00Z"
           |}
           |],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }

    "[non-doc api] create transaction" in {
      import tech.pegb.backoffice.dao.SqlDao._

      val aedId = 22
      val usdId = 18
      val euroId = 19
      val today = LocalDateTime.now().toSqlString
      val dbApi = inject[DBApi]
      val db = dbApi.database("backoffice")
      val createTodayTransction =
        s"""
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449579', 1, 10, $aedId, 'debit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449579', 2, $aedId, 10, 'credit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449579', 3, $usdId, 11, 'debit', 'currency_exchange', 15.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449579', 4, 11, $usdId, 'credit', 'currency_exchange', 15.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449580', 1, 20, $aedId, 'debit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449580', 2, $aedId, 20, 'credit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449580', 3, $usdId, 21, 'debit', 'currency_exchange', 20.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449580', 4, 21, $usdId, 'credit', 'currency_exchange', 20.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449581', 1, 10, $euroId, 'debit', 'currency_exchange', 50.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449581', 2, $euroId, 10, 'credit', 'currency_exchange', 50.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449581', 3, $usdId, 10, 'debit', 'currency_exchange', 999.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           |INSERT INTO transactions(id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
           |VALUES('2549449581', 4, 10, $usdId, 'credit', 'currency_exchange', 999.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
           |
           """.stripMargin
      db.withTransaction { implicit connection ⇒
        this.runUpdateSql(createTodayTransction)
      }

      succeed
    }

    "return currency exchange on getCurrencyExchangeById" in {

      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges/bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4").withHeaders(AuthHeader)).get
      val expected =
        """{
          |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
          |"sell_currency":"AED",
          |"buy_currency":"USD",
          |"currency_description":"US Dollar",
          |"rate":99.980000,
          |"provider":"Currency Cloud",
          |"balance":17526.50,
          |"status":"active",
          |"updated_at":"2019-02-25T00:00:00Z"
          |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return spreads linked to a given currency exchange" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges/bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4/spreads?order_by=created_at").withHeaders(AuthHeader)).get

      val expected =
        """{
          |"total":3,"results":[
          |{"id":"e5bb6aef-b2b3-4cbf-86ca-e3d858d6209b","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"currency_exchange","channel":null,"institution":null,"spread":0.150000,"updated_by":"pegbuser","updated_at":"2019-01-30T00:00:00Z"},
          |{"id":"6c5fffdd-6056-4d81-a348-34b486ce7e6a","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"international_remittance","channel":"bank","institution":null,"spread":0.250000,"updated_by":"pegbuser","updated_at":"2019-02-16T00:00:00Z"},
          |{"id":"747a1077-46ed-43a2-86b9-09817a751a44","currency_exchange_id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4","buy_currency":"USD","sell_currency":"AED","transaction_type":"currency_exchange","channel":null,"institution":null,"spread":0.200000,"updated_by":"pegbuser","updated_at":"2019-02-28T00:00:00Z"}],
          |"limit":null,"offset":null
          |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }
  }

  "Get CurrencyExchange API - Negative" should {
    "return notfoundError when currency_exchange does not exist - getCurrencyExchangeById" in {

      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges/$requestId")
        .withHeaders(Seq(("request-id" → requestId.toString), AuthHeader): _*)).get

      status(resp) mustBe NOT_FOUND
      val json = contentAsJson(resp)
      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("NotFound")
      (json \ "msg").get mustBe  JsString(s"Currency Exchange for id $requestId is not found")
    }
    "return validationError when getCurrencyExchangeById order_by contains invalid fields" in {

      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?order_by=deadbeef")
        .withHeaders(Seq(("request-id" → requestId.toString), AuthHeader): _*)).get

      status(resp) mustBe BAD_REQUEST
      val json = contentAsJson(resp)
      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("invalid field for order_by found")
    }
    "return validationError when getCurrencyExchangeById partial_match contains invalid fields" in {

      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/currency_exchanges?partial_match=deadbeef")
        .withHeaders(Seq(("request-id" → requestId.toString), AuthHeader): _*)).get

      status(resp) mustBe BAD_REQUEST
      val json = contentAsJson(resp)
      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("invalid field for partial matching found")
    }
  }

}
