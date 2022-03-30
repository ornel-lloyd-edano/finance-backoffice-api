package tech.pegb.backoffice

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import play.api.test.Helpers._

class GetLimitProfileIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "Get LimitProfile API - Positive" should {
    "return limit profile: search by uuid (balanced_based)" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles/33cd967d-5fbe-4af3-9a90-3d37488dc4b5").withHeaders(AuthHeader)).get

      val expected =
        s"""|{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"currency_code":"AED",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"max_balance_amount":10000.00,
            |"interval":null,
            |"max_amount_per_interval":null,
            |"max_amount_per_txn":null,
            |"min_amount_per_txn":null,
            |"max_count_per_interval":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }
    "return limit profile: search by uuid (transaction_based)" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles/b03b9a49-135d-4e1d-9c4c-1224667b7edc").withHeaders(AuthHeader)).get

      val expected =
        s"""|{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"currency_code":"USD",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"max_balance_amount":null,
            |"interval":"monthly",
            |"max_amount_per_interval":6000.00,
            |"max_amount_per_txn":50000.00,
            |"min_amount_per_txn":3000.00,
            |"max_count_per_interval":50000
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of limit profiles when there is no filter " in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?order_by=limit_type,-user_type").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":3,
            |"results":[
            |{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"currency_code":"AED"
            |},
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |},
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by limit_type" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?limit_type=balance_based&order_by=max_amount_per_txn").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":2,
            |"results":[
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |},
            |{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"currency_code":"AED"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by user_type" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?user_type=individual_user&order_by=tier").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":2,
            |"results":[
            |{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"currency_code":"AED"
            |},
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by tier" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?tier=tier%202&order_by=id").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":1,
            |"results":[
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by subscription" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?subscription=gold&order_by=max_amount_per_interval").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":1,
            |"results":[
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by transactionType" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?transaction_type=p2p_domestic&order_by=max_amount_per_txn").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":1,
            |"results":[
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by channel" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?channel=mobile_application&order_by=max_amount_per_txn").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":1,
            |"results":[
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by otherparty" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?other_party=Mas&order_by=interval").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":2,
            |"results":[
            |{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"currency_code":"AED"
            |},
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |}],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by instrument" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?instrument=debit_card&order_by=subscription").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":3,
            |"results":[
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |},
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |},
            |{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"currency_code":"AED"
            |}
            |],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by interval" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?interval=daily&order_by=-max_amount_per_txn").withHeaders(AuthHeader)).get

      val expected =
        s"""|{"total":2,
            |"results":[
            |{
            |"id":"33cd967d-5fbe-4af3-9a90-3d37488dc4b5",
            |"limit_type":"balance_based",
            |"user_type":"individual_user",
            |"tier":"tier 1",
            |"subscription":"standard",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-03-31T13:09:39Z",
            |"currency_code":"AED"
            |},
            |{"id":"c1cdb0af-4940-4fe0-b3f5-320a508c7f8b",
            |"limit_type":"balance_based",
            |"user_type":"business_user",
            |"tier":"tier 1",
            |"subscription":"gold",
            |"transaction_type":"p2p_domestic",
            |"channel":"mobile_application",
            |"other_party":null,
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"AED"
            |}
            |],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return list of profiles filter by currency_code" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?currency_code=USD&order_by=instrument").withHeaders(AuthHeader)).get
      val expected =
        s"""|{"total":1,
            |"results":[
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |}
            |],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }
    "return list of profiles multiple filters" in {
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?currency_code=USD&limit_type=transaction_based&order_by=instrument").withHeaders(AuthHeader)).get
      val expected =
        s"""|{"total":1,
            |"results":[
            |{
            |"id":"b03b9a49-135d-4e1d-9c4c-1224667b7edc",
            |"limit_type":"transaction_based",
            |"user_type":"individual_user",
            |"tier":"tier 2",
            |"subscription":"platinum",
            |"transaction_type":"top-up",
            |"channel":"atm",
            |"other_party":"Mashreq",
            |"instrument":"debit_card",
            |"updated_at":"2019-02-20T00:00:00Z",
            |"currency_code":"USD"
            |}
            |],
            |"limit":null,
            |"offset":null
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }
  }

  "Get LimitProfile API - Negative" should {
    "return limit profile: search by uuid" in {
      val requestId = UUID.randomUUID()
      val fakeUUID = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles/$fakeUUID")
        .withHeaders(Seq(("request-id" → requestId.toString), AuthHeader): _*)).get

      /*val expected =
        s"""|{
            |"id":"$requestId",
            |"code":"NotFound",
            |"msg":"LimitProfile with uuid $fakeUUID not found"
            |}""".stripMargin.replaceAll(System.lineSeparator(), "")*/

      status(resp) mustBe NOT_FOUND
      //contentAsString(resp) mustEqual expected
      val json = contentAsJson(resp)
      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("NotFound")
      (json \ "msg").get.toString().contains(s"LimitProfile with uuid $fakeUUID not found") mustBe true
    }

    "respond error when orderBy contain invalid" in {
      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?order_by=deadbeef")
        .withHeaders(Seq(("request-id" → requestId.toString), AuthHeader): _*)).get

      val json = contentAsJson(resp)

      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("invalid field for order_by found.") mustBe true
    }

    "respond error when partial_match contain invalid" in {
      val requestId = UUID.randomUUID()
      val resp = route(app, FakeRequest(GET, s"/api/limit_profiles?partial_match=deadbeef")
        .withHeaders(Seq(("request-id" → requestId.toString), AuthHeader): _*)).get

      val json = contentAsJson(resp)

      (json \ "id").get mustBe JsString(requestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("invalid field for partial matching found.") mustBe true
    }

  }
}
