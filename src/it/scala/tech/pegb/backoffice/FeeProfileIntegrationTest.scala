package tech.pegb.backoffice

import java.time._
import java.util.UUID

import org.scalatest.Matchers._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.fee.dto.FeeProfileToReadDetails
import tech.pegb.backoffice.api.json.Implicits._

class FeeProfileIntegrationTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  private val basePath = "/api/fee_profiles"

  private var feeProfileId: UUID = _
  private val requestId = UUID.randomUUID()

  "Fee profiles get by uuid" should {
    "return fee profile for existing uuid(flat_fee)" in {
      val id = "78fcb677-d0a6-4074-aac2-c2c7e76e9b25"
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles/$id")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected =
        """{
          |"id":"78fcb677-d0a6-4074-aac2-c2c7e76e9b25",
          |"fee_type":"transaction_based",
          |"user_type":"individual",
          |"tier":"basic",
          |"subscription_type":"standard",
          |"transaction_type":"p2p_domestic",
          |"channel":"mobile_application",
          |"other_party":null,
          |"instrument":"visa_debit",
          |"calculation_method":"flat_fee",
          |"currency_code":"AED",
          |"fee_method":"add",
          |"tax_included":true,
          |"max_fee":null,
          |"min_fee":null,
          |"fee_amount":20.00,
          |"fee_ratio":null,
          |"ranges":null,
          |"updated_at":"2019-02-21T00:00:00Z"
          |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
    "return fee profile for existing uuid (staircase_percentage)" in {
      val id = "95447383-b2a6-4be4-b601-261d235dbb6b"
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles/$id")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected =
        """{
          |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
          |"fee_type":"transaction_based",
          |"user_type":"business",
          |"tier":"small",
          |"subscription_type":"gold",
          |"transaction_type":"p2p_domestic",
          |"channel":"mobile_application",
          |"other_party":null,
          |"instrument":"visa_debit",
          |"calculation_method":"staircase_flat_percentage",
          |"currency_code":"USD",
          |"fee_method":"add",
          |"tax_included":false,
          |"max_fee":10.00,
          |"min_fee":5.00,
          |"fee_amount":null,
          |"fee_ratio":null,
          |"ranges":[
          |{
          |"id":1,
          |"max":1000,
          |"min":0,
          |"fee_amount":null,
          |"fee_ratio":0.0005
          |},
          |{
          |"id":2,
          |"max":5000,
          |"min":1001,
          |"fee_amount":null,
          |"fee_ratio":0.0002
          |},
          |{
          |"id":3,
          |"max":10000,
          |"min":5001,
          |"fee_amount":null,
          |"fee_ratio":0.0001
          |}],
          |"updated_at":"2019-02-21T00:00:00Z"}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
  }

  "Fee profiles get by criteria" should {
    "return list of profiles without filter except no active currency" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?order_by=fee_type,user_type")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
          |{"total":3,
          |"results":[
          |{
          |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
          |"fee_type":"subscription_based",
          |"user_type":"individual",
          |"tier":"basic",
          |"subscription_type":"platinum",
          |"transaction_type":"p2p_international",
          |"channel":"atm",
          |"other_party":"Mashreq",
          |"instrument":"visa_debit",
          |"calculation_method":"flat_fee",
          |"fee_method":"deduct",
          |"tax_included":null,
          |"currency_code":"AED",
          |"updated_at":"2019-02-21T00:00:00Z"
          |},
          |{
          |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
          |"fee_type":"transaction_based",
          |"user_type":"business",
          |"tier":"small",
          |"subscription_type":"gold",
          |"transaction_type":"p2p_domestic",
          |"channel":"mobile_application",
          |"other_party":null,
          |"instrument":"visa_debit",
          |"calculation_method":"staircase_flat_percentage",
          |"fee_method":"add",
          |"tax_included":false,
          |"currency_code":"USD",
          |"updated_at":"2019-02-21T00:00:00Z"
          |},
          |{
          |"id":"78fcb677-d0a6-4074-aac2-c2c7e76e9b25",
          |"fee_type":"transaction_based",
          |"user_type":"individual",
          |"tier":"basic",
          |"subscription_type":"standard",
          |"transaction_type":"p2p_domestic",
          |"channel":"mobile_application",
          |"other_party":null,
          |"instrument":"visa_debit",
          |"calculation_method":"flat_fee",
          |"fee_method":"add",
          |"tax_included":true,
          |"currency_code":"AED",
          |"updated_at":"2019-02-21T00:00:00Z"}],
          |"limit":null,
          |"offset":null
          |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of profiles filter by fee_type" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?fee_type=transaction_based&order_by=subscription_type")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":2,
                       |"results":[
                       |{
                       |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
                       |"fee_type":"transaction_based",
                       |"user_type":"business",
                       |"tier":"small",
                       |"subscription_type":"gold",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"staircase_flat_percentage",
                       |"fee_method":"add",
                       |"tax_included":false,
                       |"currency_code":"USD",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |},
                       |{
                       |"id":"78fcb677-d0a6-4074-aac2-c2c7e76e9b25",
                       |"fee_type":"transaction_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"standard",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"add",
                       |"tax_included":true,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of profiles filter by user_type" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?user_type=individual&order_by=-transaction_type")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":2,
                       |"results":[
                       |{
                       |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
                       |"fee_type":"subscription_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"platinum",
                       |"transaction_type":"p2p_international",
                       |"channel":"atm",
                       |"other_party":"Mashreq",
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"deduct",
                       |"tax_included":null,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |},
                       |{
                       |"id":"78fcb677-d0a6-4074-aac2-c2c7e76e9b25",
                       |"fee_type":"transaction_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"standard",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"add",
                       |"tax_included":true,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of profiles filter by transaction_type" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?transaction_type=p2p_international")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":1,
                       |"results":[
                       |{
                       |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
                       |"fee_type":"subscription_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"platinum",
                       |"transaction_type":"p2p_international",
                       |"channel":"atm",
                       |"other_party":"Mashreq",
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"deduct",
                       |"tax_included":null,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of profiles filter by channel" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?channel=atm&order_by=-other_party")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":1,
                       |"results":[
                       |{
                       |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
                       |"fee_type":"subscription_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"platinum",
                       |"transaction_type":"p2p_international",
                       |"channel":"atm",
                       |"other_party":"Mashreq",
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"deduct",
                       |"tax_included":null,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
    "return list of profiles filter by other_party (exact)" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?other_party=Mashreq&order_by=-other_party")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":1,
                       |"results":[
                       |{
                       |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
                       |"fee_type":"subscription_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"platinum",
                       |"transaction_type":"p2p_international",
                       |"channel":"atm",
                       |"other_party":"Mashreq",
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"deduct",
                       |"tax_included":null,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
    "return list of profiles filter by other_party (partial)" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?other_party=Mas&partial_match=other_party&order_by=-other_party")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":1,
                       |"results":[
                       |{
                       |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
                       |"fee_type":"subscription_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"platinum",
                       |"transaction_type":"p2p_international",
                       |"channel":"atm",
                       |"other_party":"Mashreq",
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"deduct",
                       |"tax_included":null,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
    "return list of profiles filter by instrument" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?instrument=visa_debit&order_by=-calculation_method,-fee_method")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":3,
                       |"results":[
                       |{
                       |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
                       |"fee_type":"transaction_based",
                       |"user_type":"business",
                       |"tier":"small",
                       |"subscription_type":"gold",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"staircase_flat_percentage",
                       |"fee_method":"add",
                       |"tax_included":false,
                       |"currency_code":"USD",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |},
                       |{
                       |"id":"acd39ec4-d76e-41d1-bc07-dca44403059d",
                       |"fee_type":"subscription_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"platinum",
                       |"transaction_type":"p2p_international",
                       |"channel":"atm",
                       |"other_party":"Mashreq",
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"deduct",
                       |"tax_included":null,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |},
                       |{
                       |"id":"78fcb677-d0a6-4074-aac2-c2c7e76e9b25",
                       |"fee_type":"transaction_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"standard",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"add",
                       |"tax_included":true,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of profiles filter by calculationMethod" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?calculation_method=staircase_flat_percentage")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":1,
                       |"results":[
                       |{
                       |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
                       |"fee_type":"transaction_based",
                       |"user_type":"business",
                       |"tier":"small",
                       |"subscription_type":"gold",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"staircase_flat_percentage",
                       |"fee_method":"add",
                       |"tax_included":false,
                       |"currency_code":"USD",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }

    "return list of profiles filter by feeMethod" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?fee_method=add&order_by=currency_code")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":2,
                       |"results":[
                       |{
                       |"id":"78fcb677-d0a6-4074-aac2-c2c7e76e9b25",
                       |"fee_type":"transaction_based",
                       |"user_type":"individual",
                       |"tier":"basic",
                       |"subscription_type":"standard",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"flat_fee",
                       |"fee_method":"add",
                       |"tax_included":true,
                       |"currency_code":"AED",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |},
                       |{
                       |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
                       |"fee_type":"transaction_based",
                       |"user_type":"business",
                       |"tier":"small",
                       |"subscription_type":"gold",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"staircase_flat_percentage",
                       |"fee_method":"add",
                       |"tax_included":false,
                       |"currency_code":"USD",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
    "return list of profiles filter by currencyCode" in {
      val resp = route(app, FakeRequest(GET, s"/api/fee_profiles?currency_code=USD")
        .withHeaders(Seq(("request-id", requestId.toString), AuthHeader): _*)).get

      val expected = s"""
                       |{"total":1,
                       |"results":[
                       |{
                       |"id":"95447383-b2a6-4be4-b601-261d235dbb6b",
                       |"fee_type":"transaction_based",
                       |"user_type":"business",
                       |"tier":"small",
                       |"subscription_type":"gold",
                       |"transaction_type":"p2p_domestic",
                       |"channel":"mobile_application",
                       |"other_party":null,
                       |"instrument":"visa_debit",
                       |"calculation_method":"staircase_flat_percentage",
                       |"fee_method":"add",
                       |"tax_included":false,
                       |"currency_code":"USD",
                       |"updated_at":"2019-02-21T00:00:00Z"
                       |}],
                       |"limit":null,
                       |"offset":null
                       |}""".stripMargin.replace(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected
    }
  }

  "Fee profiles API" should {
    var updatedAt: Option[ZonedDateTime] = None
    "create fee profile should fail when ranges has gap" in {
      val jsonPayload = s"""{
                           |  "fee_type": "transaction_based",
                           |  "user_type": "individual",
                           |  "tier":"basic",
                           |  "subscription_type": "standard",
                           |  "transaction_type": "currency_exchange",
                           |  "channel": "mobile_application",
                           |  "other_party": "Mashreq",
                           |  "instrument": "debit_card",
                           |  "calculation_method": "staircase_flat_fee",
                           |  "currency_code": "CHF",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "ranges": [
                           |    {
                           |      "max": 50,
                           |      "min": 0,
                           |      "fee_amount": 7
                           |    },
                           |    {
                           |      "max": 100,
                           |      "min": 51,
                           |      "fee_amount": 7
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val expected = "Current range 'from' value (51) should be equal to Previous Range 'to' value (50)"
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "msg").get.toString() should include(expected)

      status(resp) mustBe BAD_REQUEST

    }

    "create fee profile" in {
      val jsonPayload = s"""{
                           |  "fee_type": "transaction_based",
                           |  "user_type": "individual",
                           |  "tier":"basic",
                           |  "subscription_type": "standard",
                           |  "transaction_type": "currency_exchange",
                           |  "channel": "mobile_application",
                           |  "other_party": "Mashreq",
                           |  "instrument": "debit_card",
                           |  "calculation_method": "staircase_flat_fee",
                           |  "currency_code": "CHF",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "ranges": [
                           |    {
                           |      "max": 50,
                           |      "min": 0,
                           |      "fee_amount": 7
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.foreach(f ⇒ feeProfileId = f.id)
      jsonResponse.map(_.ranges.map(_.size)) mustBe Right(Some(1))
      status(resp) mustBe CREATED
      succeed
    }

    "create fee profile should fail when currency is inactive" in {
      val jsonPayload = s"""{
                           |  "fee_type": "transaction_based",
                           |  "user_type": "individual",
                           |  "tier":"basic",
                           |  "subscription_type": "standard",
                           |  "transaction_type": "currency_exchange",
                           |  "channel": "mobile_application",
                           |  "other_party": "Mashreq",
                           |  "instrument": "debit_card",
                           |  "calculation_method": "staircase_flat_fee",
                           |  "currency_code": "PHP",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "ranges": [
                           |    {
                           |      "max": 50,
                           |      "min": 0,
                           |      "fee_amount": 7
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("no active currency id found for code PHP")
    }

    "get that created fee profile" in {

      val request = FakeRequest(GET, basePath + s"/$feeProfileId").withHeaders(AuthHeader)
      val resp = route(app, request).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.map(_.id) mustBe Right(feeProfileId)
      updatedAt = jsonResponse.right.get.updatedAt
    }

    "update fee profile" in {
      val jsonPayload = s"""{
                           |	"calculation_method": "staircase_flat_fee",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "updated_at": ${updatedAt.fold("null")(t ⇒ s""""$t"""")},
                           |  "ranges": [
                           |    {
                           |      "max": 40,
                           |      "min": 0,
                           |      "flat_amount": 5
                           |    },
                           |    {
                           |      "max": 100,
                           |      "min": 40,
                           |      "flat_amount": 8
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      
      val request = FakeRequest(PUT, basePath + s"/$feeProfileId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.map(_.ranges.map(_.size)) mustBe Right(Some(2))
    }

    "return 412 on update limit profile which is recently updated (precondition fail)" in {
      val jsonPayload = s"""{
                           |	"calculation_method": "staircase_flat_fee",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "ranges": [
                           |    {
                           |      "max": 40,
                           |      "min": 0,
                           |      "flat_amount": 5
                           |    },
                           |    {
                           |      "max": 100,
                           |      "min": 40,
                           |      "flat_amount": 8
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(PUT, basePath + s"/$feeProfileId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe PRECONDITION_FAILED
    }

    "return 412 on delete limit profile which is recently updated (precondition fail)" in {
      val finalUpdatedAt = ZonedDateTime.now(Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault()))

      val jsonRequest =
        s"""{
           |"updated_at": "${finalUpdatedAt}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(DELETE, basePath + s"/$feeProfileId", jsonHeaders, jsonRequest)
      val resp = route(app, request).get

      status(resp) mustBe PRECONDITION_FAILED
    }

    "delete fee profile" in {
      val getRequest = FakeRequest(GET, basePath + s"/$feeProfileId").withHeaders(AuthHeader)
      val getResp = route(app, getRequest).get

      status(getResp) mustBe OK
      val jsonResponse = contentAsString(getResp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.map(_.id) mustBe Right(feeProfileId)

      val lastUpdatedAt = jsonResponse.map(_.updatedAt).right.get

      val jsonRequest =
        s"""{
           |"updated_at": "${lastUpdatedAt.getOrElse(null)}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(DELETE, basePath + s"/$feeProfileId", jsonHeaders, jsonRequest.toString)
      val resp = route(app, request).get

      status(resp) mustBe OK

      val checkRequest = FakeRequest(GET, basePath + s"/$feeProfileId").withHeaders(AuthHeader)
      val checkResp = route(app, checkRequest).get
      status(checkResp) mustBe NOT_FOUND

    }
  }

  "FeeProfile create and change calculation method" should {
    var updatedAt: Option[ZonedDateTime] = None
    "create fee profile" in {
      val jsonPayload = s"""{
                           |  "fee_type": "subscription_based",
                           |  "user_type": "individual",
                           |  "tier":"basic",
                           |  "subscription_type": "standard",
                           |  "transaction_type": "currency_exchange",
                           |  "channel": "mobile_application",
                           |  "other_party": "Mashreq",
                           |  "instrument": "debit_card",
                           |  "calculation_method": "flat_fee",
                           |  "currency_code": "CHF",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "max_fee":null,
                           |  "min_fee":null,
                           |  "fee_amount":20.00,
                           |  "fee_ratio":null,
                           |  "ranges":null
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.foreach(f ⇒ feeProfileId = f.id)
      status(resp) mustBe CREATED
      succeed
    }

    "get that created fee profile" in {
      val request = FakeRequest(GET, basePath + s"/$feeProfileId").withHeaders(AuthHeader)
      val resp = route(app, request).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.map(_.id) mustBe Right(feeProfileId)
      updatedAt = jsonResponse.right.get.updatedAt
    }

    "update fee profile fails when theres a gap on ranges" in {
      val jsonPayload = s"""{
                           |	"calculation_method": "staircase_flat_fee",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "updated_at": ${updatedAt.fold("null")(t ⇒ s""""$t"""")},
                           |  "fee_amount": null,
                           |  "ranges": [
                           |    {
                           |      "max": 40,
                           |      "min": 0,
                           |      "flat_amount": 5
                           |    },
                           |    {
                           |      "max": 100,
                           |      "min": 41,
                           |      "flat_amount": 8
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(PUT, basePath + s"/$feeProfileId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val expected = "Current range 'from' value (41) should be equal to Previous Range 'to' value (40)"
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "msg").get.toString() should include(expected)

      status(resp) mustBe BAD_REQUEST
    }

    "update fee profile" in {
      val jsonPayload = s"""{
                           |	"calculation_method": "staircase_flat_fee",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "updated_at": ${updatedAt.fold("null")(t ⇒ s""""$t"""")},
                           |  "fee_amount": null,
                           |  "ranges": [
                           |    {
                           |      "max": 40,
                           |      "min": 0,
                           |      "flat_amount": 5
                           |    },
                           |    {
                           |      "max": 100,
                           |      "min": 40,
                           |      "flat_amount": 8
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(PUT, basePath + s"/$feeProfileId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.map(_.ranges.map(_.size)) mustBe Right(Some(2))
    }

    "get that updated fee profile" in {
      val request = FakeRequest(GET, basePath + s"/$feeProfileId").withHeaders(AuthHeader)
      val resp = route(app, request).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.map(_.id) mustBe Right(feeProfileId)
      jsonResponse.map(_.calculationMethod) mustBe Right("staircase_flat_fee")
      jsonResponse.map(_.ranges.map(_.size)) mustBe Right(Some(2))

    }

    "create a fee profile with null channel" in {
      val jsonPayload = s"""{
                           |  "fee_type": "transaction_based",
                           |  "user_type": "individual",
                           |  "tier":"basic",
                           |  "subscription_type": "standard",
                           |  "transaction_type": "currency_exchange",
                           |  "channel": null,
                           |  "other_party": "Mashreq",
                           |  "instrument": "debit_card",
                           |  "calculation_method": "staircase_flat_fee",
                           |  "currency_code": "CHF",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "ranges": [
                           |    {
                           |      "max": 50,
                           |      "min": 0,
                           |      "fee_amount": 7
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[FeeProfileToReadDetails]).toEither
      jsonResponse.foreach(f ⇒ feeProfileId = f.id)
      jsonResponse.map(_.ranges.map(_.size)) mustBe Right(Some(1))
      jsonResponse.map(_.channel) mustBe Right(None)
      status(resp) mustBe CREATED
      succeed
    }

    "create a identical fee profile with null channel should return error" in {
      val jsonPayload = s"""{
                           |  "fee_type": "transaction_based",
                           |  "user_type": "individual",
                           |  "tier":"basic",
                           |  "subscription_type": "standard",
                           |  "transaction_type": "currency_exchange",
                           |  "channel": null,
                           |  "other_party": "Mashreq",
                           |  "instrument": "debit_card",
                           |  "calculation_method": "staircase_flat_fee",
                           |  "currency_code": "CHF",
                           |  "fee_method": "add",
                           |  "tax_included": true,
                           |  "ranges": [
                           |    {
                           |      "max": 50,
                           |      "min": 0,
                           |      "fee_amount": 7
                           |    }
                           |  ]
                           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "msg").get.toString() should include("Fee profile with same features already exists")

      status(resp) mustBe BAD_REQUEST
      succeed
    }

  }

}
