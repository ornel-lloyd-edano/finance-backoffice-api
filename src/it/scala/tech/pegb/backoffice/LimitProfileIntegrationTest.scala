package tech.pegb.backoffice

import anorm._
import java.time._
import java.util.UUID

import org.scalatest.Matchers._
import play.api.db.DBApi
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.limit.dto.LimitProfileToReadDetail

class LimitProfileIntegrationTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  private val basePath = "/api/limit_profiles"

  private var limitId: UUID = _

  val db = inject[DBApi].database("backoffice")

  "Limit profiles API" should {
    var updatedAt: Option[ZonedDateTime] = None

    "create limit profile fail when currency is inactive" in {
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "PHP",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("no active currency id found for code PHP")
    }

    "create limit profile" in {
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp) mustBe CREATED
      jsonResponse.foreach(p ⇒ limitId = p.id)
    }

    "get that created limit profile" in {
      val request = FakeRequest(GET, basePath + s"/$limitId").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.id) mustBe Right(limitId)
      status(resp) mustBe OK
      updatedAt = jsonResponse.right.get.updatedAt
    }

    "update limit profile" in {
      val jsonPayload =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": 50,
           |	"max_count_per_interval": 2,
           |  "updated_at": ${updatedAt.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(PUT, basePath + s"/$limitId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.id) mustBe Right(limitId)
      status(resp) mustBe OK
    }

    "return 412 on update limit profile which is recently updated (precondition fail)" in {
      val jsonPayload =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": 50,
           |	"max_count_per_interval": 2
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(PUT, basePath + s"/$limitId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe PRECONDITION_FAILED
    }

    "return 412 on delete limit profile which is recently updated (precondition fail)" in {
      val getRequest = FakeRequest(GET, basePath + s"/$limitId").withHeaders(AuthHeader)
      val getResp = route(app, getRequest).get

      val jsonResponse = contentAsString(getResp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.id) mustBe Right(limitId)
      status(getResp) mustBe OK

      val finalUpdatedAt = ZonedDateTime.now(Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault()))

      val jsonPayload =
        s"""{
           |"updated_at": "$finalUpdatedAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(DELETE, basePath + s"/$limitId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe PRECONDITION_FAILED
    }

    "delete limit profile" in {
      val getRequest = FakeRequest(GET, basePath + s"/$limitId").withHeaders(AuthHeader)
      val getResp = route(app, getRequest).get

      val jsonResponse = contentAsString(getResp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.id) mustBe Right(limitId)
      status(getResp) mustBe OK

      val lastUpdatedAt = jsonResponse.map(_.updatedAt).right.get
      val jsonPayload =
        s"""{
           |"updated_at": "${lastUpdatedAt.orNull}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(DELETE, basePath + s"/$limitId", jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe OK

      val checkRequest = FakeRequest(GET, basePath + s"/$limitId").withHeaders(AuthHeader)
      val checkResp = route(app, checkRequest).get
      status(checkResp) mustBe NOT_FOUND
    }

    "return error when creating monthly(max_amount_per_interval, max_count_per_interval) and update all amount null [PWBOAPI-645]" in {

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp) mustBe CREATED

      val monthlyId = jsonResponse.map(_.id).right.get
      val updateAt2 = jsonResponse.map(_.updatedAt).right.get

      val ujs1 =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req3 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs1)
      val resp3 = route(app, req3).get

      status(resp3) mustBe BAD_REQUEST
      val jsResp3 = contentAsJson(resp3)
      (jsResp3 \ "code").get.toString should include("InvalidRequest")
      (jsResp3 \ "msg").get.toString should include("assertion failed: At least one of: (max interval amount, max count, min amount, max amount) have to be specified for transaction_based limit type")


      val ujs2 =
        s"""{
           |	"max_amount_per_interval": 10.00,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": 10,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req4 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs2)
      val resp4 = route(app, req4).get

      status(resp4) mustBe BAD_REQUEST
      val jsResp4 = contentAsJson(resp4)
      (jsResp4 \ "code").get.toString should include("InvalidRequest")
      (jsResp4 \ "msg").get.toString should include("assertion failed: Max balance have to be left empty for transaction_based limit type")


      //get
      val getRequest = FakeRequest(GET, basePath + s"/$monthlyId").withHeaders(AuthHeader)
      val getResp = route(app, getRequest).get

      val getJsonResponse = contentAsString(getResp).as(classOf[LimitProfileToReadDetail]).toEither
      getJsonResponse.map(_.id) mustBe Right(monthlyId)
      status(getResp) mustBe OK
      getJsonResponse.map(_.maxAmountPerInterval) mustBe Right(Some(BigDecimal(30.0)))

      val cleanResult = db.withConnection { implicit conn ⇒
        val idToDelete: Int = SQL(
          """
            |SELECT MAX(id) FROM limit_profiles
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].single)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id = $idToDelete
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id = $idToDelete
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true

    }

    "return error when creating monthly(balance_based(max_balance_amount)) and update all amount null [PWBOAPI-645]" in {

      val jsonPayload =
        s"""{
           |	"limit_type": "balance_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": null,
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": 50.00
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp) mustBe CREATED
      val monthlyId = jsonResponse.map(_.id).right.get
      val updateAt2 = jsonResponse.map(_.updatedAt).right.get

      val ujs1 =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           | "max_balance_amount": null,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req3 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs1)
      val resp3 = route(app, req3).get

      status(resp3) mustBe BAD_REQUEST
      val jsResp3 = contentAsJson(resp3)
      (jsResp3 \ "code").get.toString should include("InvalidRequest")
      (jsResp3 \ "msg").get.toString should include("assertion failed: Max balance have to be specified for balance_based limit type")

      val ujs2 =
        s"""{
           |	"max_amount_per_interval": 10.00,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": 10,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req4 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs2)
      val resp4 = route(app, req4).get

      status(resp4) mustBe BAD_REQUEST
      val jsResp4 = contentAsJson(resp4)
      (jsResp4 \ "code").get.toString should include("InvalidRequest")
      (jsResp4 \ "msg").get.toString should include("assertion failed: Max interval amount have to be left empty for balance_based limit type")

      //get
      val getRequest = FakeRequest(GET, basePath + s"/$monthlyId").withHeaders(AuthHeader)
      val getResp = route(app, getRequest).get

      val getJsonResponse = contentAsString(getResp).as(classOf[LimitProfileToReadDetail]).toEither
      getJsonResponse.map(_.id) mustBe Right(monthlyId)
      status(getResp) mustBe OK
      getJsonResponse.map(_.maxBalanceAmount) mustBe Right(Some(BigDecimal(50.0)))

      val cleanResult = db.withConnection { implicit conn ⇒
        val idToDelete: Int = SQL(
          """
            |SELECT MAX(id) FROM limit_profiles
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].single)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id = $idToDelete
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id = $idToDelete
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return success when creating daily(max_amount_per_interval, max_count_per_interval) with (30.00, null) and monthly with (null, 1) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_3",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp) mustBe CREATED

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true

      succeed
    }

    "return success when creating daily(max_amount_per_interval, max_count_per_interval) with (null, 5) and monthly with (30.00, null) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           | "tier": "tier_1",
           | "subscription": "gold",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 5,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_3",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe CREATED

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)
        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true

      succeed
    }

    "return success when creating daily(max_amount_per_interval, max_count_per_interval) with (20.00, 5) and monthly with (30.00, 10) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 5,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_2",
           |	"subscription": "gold",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe CREATED
      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
      succeed
    }

    "return fail when creating daily(max_amount_per_interval, max_count_per_interval) with (null, 5) and monthly with (null, 1) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "platinum",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 5,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "platinum",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe BAD_REQUEST
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "code").get.toString should include("InvalidRequest")
      (jsonResponse \ "msg").get.toString should include("max interval count can not be less than or equal to 5 for monthly interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 1
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)
        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return success when creating daily(max_amount_per_interval, max_count_per_interval) with (20.00, 5) and monthly with (30.00, null), update monthly to (null, 10) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 5,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp) mustBe CREATED
      val monthlyId = jsonResponse.map(_.id).right.get
      val updateAt2 = jsonResponse.map(_.updatedAt).right.get


      val ujs1 =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": 10,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req3 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs1)
      val resp3 = route(app, req3).get

      val jsonResponse3 = contentAsString(resp3).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse3.map(_.maxAmountPerInterval) mustBe Right(None)
      jsonResponse3.map(_.maxCountPerInterval) mustBe Right(Some(10))
      status(resp3) mustBe OK

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)
        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return success when creating daily(max_amount_per_interval, max_count_per_interval) with (20.00, 5) and monthly with (30.00, null), update daily to (null, 10) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 5,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      val jsonResponse2 = contentAsString(resp2).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp2) mustBe CREATED
      val dailyId = jsonResponse2.map(_.id).right.get
      val updateAt2 = jsonResponse2.map(_.updatedAt).right.get


      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe CREATED


      val ujs1 =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": 10,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req3 = FakeRequest(PUT, basePath + s"/$dailyId", jsonHeaders, ujs1)
      val resp3 = route(app, req3).get

      val jsonResponse3 = contentAsString(resp3).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse3.map(_.maxAmountPerInterval) mustBe Right(None)
      jsonResponse3.map(_.maxCountPerInterval) mustBe Right(Some(10))
      status(resp3) mustBe OK

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)
        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error when creating daily(max_amount_per_interval, max_count_per_interval) with (20.00, 5) and monthly with (30.00, null), update monthly to (null, 1) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 5,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp) mustBe CREATED
      val monthlyId = jsonResponse.map(_.id).right.get
      val updateAt2 = jsonResponse.map(_.updatedAt).right.get


      val ujs1 =
        s"""{
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": 1,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req3 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs1)
      val resp3 = route(app, req3).get

      status(resp3) mustBe BAD_REQUEST
      val jsResp3 = contentAsJson(resp3)
      (jsResp3 \ "code").get.toString should include("InvalidRequest")
      (jsResp3 \ "msg").get.toString should include("max interval count can not be less than or equal to 5 for monthly interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error when creating monthly(max_amount_per_interval, max_count_per_interval) with (null, 10) and daily with (null, 15) [PWBOAPI-633]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": 10,
           | "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      status(resp2) mustBe CREATED
      succeed

      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": 15,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe BAD_REQUEST
      val jsResp3 = contentAsJson(resp)
      (jsResp3 \ "code").get.toString should include("InvalidRequest")
      (jsResp3 \ "msg").get.toString should include("max interval count can not be more than or equal to 10 for daily interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 1
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "create transaction_based then balance_based limit profile [PWBOAPI_631]" in {
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_2",
           |	"subscription": "standard",
           |	"transaction_type": null,
           |	"channel": null,
           |	"other_party": null,
           |	"instrument": null,
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp) mustBe CREATED
      succeed

      val jsonPayload2 =
        s"""{
           |	"limit_type": "balance_based",
           |	"user_type": "individual",
           |	"tier": "tier_2",
           |	"subscription": "standard",
           |	"transaction_type": null,
           |	"channel": null,
           |	"other_party": null,
           |	"instrument": null,
           |	"interval": null,
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": 100.00
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsonPayload2)
      val resp2 = route(app, request2).get

      val jsonResponse2 = contentAsString(resp2).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse2.map(_.maxBalanceAmount) mustBe Right(Some(100))
      status(resp2) mustBe CREATED
      succeed

      val jsonPayload3 =
        s"""{
           |	"limit_type": "balance_based",
           |	"user_type": "individual",
           |	"tier": "tier_2",
           |	"subscription": "standard",
           |	"transaction_type": null,
           |	"channel": null,
           |	"other_party": null,
           |	"instrument": null,
           |	"interval": null,
           |  "currency_code": "AED",
           |	"max_amount_per_interval": null,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": null,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": 150.00
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request3 = FakeRequest(POST, basePath, jsonHeaders, jsonPayload3)
      val resp3 = route(app, request3).get

      val jsonResponse3 = contentAsJson(resp3)
      (jsonResponse3 \ "code").get.toString should include("InvalidRequest")
      (jsonResponse3 \ "msg").get.toString should include("Limit profile with same features already exists")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

      SQL(
        s"""
           |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
         """.stripMargin).executeUpdate()
      val result = SQL(
        s"""
           |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
         """.stripMargin).executeUpdate()
      result > 0
      }
      cleanResult mustBe true
    }

    "return success when creating ALL instrument, then specific [PWBOAPI-631]" in {
      //create specif instrument (debit_card)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      val jsonResponse2 = contentAsString(resp2).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse2.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp2) mustBe CREATED
      succeed

      //instrument = ALL [null]
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": null,
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp) mustBe CREATED

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return success when creating ALL channel, then specific [PWBOAPI-631]" in {
      //create specif channel (mobile_application)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      val jsonResponse2 = contentAsString(resp2).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse2.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp2) mustBe CREATED
      succeed

      //channel = ALL [null]
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": null,
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp) mustBe CREATED

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return success when creating null other_party, then specific [PWBOAPI-631]" in {
      //create specific other_party (PWBOAPI_631_c)
      val jsp2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_2",
           |	"subscription": "gold",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request2 = FakeRequest(POST, basePath, jsonHeaders, jsp2)
      val resp2 = route(app, request2).get

      val jsonResponse2 = contentAsString(resp2).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse2.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp2) mustBe CREATED
      succeed

      //other_party = [null]
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": null,
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      val jsonResponse = contentAsString(resp).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponse.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp) mustBe CREATED

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 2
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error when creating identical limit profile" in {
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      status(resp) mustBe CREATED

      val sameRequest = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp2 = route(app, request).get

      status(resp2) mustBe BAD_REQUEST

      val jsonResponse = contentAsJson(resp2)
      (jsonResponse \ "code").get.toString should include("InvalidRequest")
      (jsonResponse \ "msg").get.toString should include("Limit profile with same features already exists")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT MAX(id) FROM limit_profiles
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error when monthly and yearly's max_amount per interval is < daily " in {
      val jsonDaily =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestDaily = FakeRequest(POST, basePath, jsonHeaders, jsonDaily)
      val respDaily = route(app, requestDaily).get

      val jsonResponseDaily = contentAsString(respDaily).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponseDaily.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(respDaily) mustBe CREATED

      val jsonMonthly =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 10.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestMonthly = FakeRequest(POST, basePath, jsonHeaders, jsonMonthly)
      val respMonthly = route(app, requestMonthly).get

      status(respMonthly) mustBe BAD_REQUEST
      val jsonResponseMonthly = contentAsJson(respMonthly)
      (jsonResponseMonthly \ "code").get.toString should include("InvalidRequest")
      (jsonResponseMonthly \ "msg").get.toString should include("max interval amount can not be less than or equal to 20.00 for monthly interval")


      val jsonYearly =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "yearly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 10.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestYearly = FakeRequest(POST, basePath, jsonHeaders, jsonYearly)
      val respYearly = route(app, requestYearly).get

      status(respYearly) mustBe BAD_REQUEST
      val jsonResponseYearly = contentAsJson(respYearly)
      (jsonResponseYearly \ "code").get.toString should include("InvalidRequest")
      (jsonResponseYearly \ "msg").get.toString should include("max interval amount can not be less than or equal to 20.00 for yearly interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 1
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error when daily's max_amount > monthly's and yearly's max_amount is < monthly " in {
      val jsonMonthly =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestMonthly = FakeRequest(POST, basePath, jsonHeaders, jsonMonthly)
      val respMonthly = route(app, requestMonthly).get

      val jsonResponseMonthly = contentAsString(respMonthly).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponseMonthly.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(respMonthly) mustBe CREATED

      val jsonDaily =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestDaily = FakeRequest(POST, basePath, jsonHeaders, jsonDaily)
      val respDaily = route(app, requestDaily).get

      status(respDaily) mustBe BAD_REQUEST
      val jsonResponseDaily = contentAsJson(respDaily)
      (jsonResponseDaily \ "code").get.toString should include("InvalidRequest")
      (jsonResponseDaily \ "msg").get.toString should include("max interval amount can not be more than or equal to 20.00 for daily interval")

      val jsonYearly =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "yearly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 10.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestYearly = FakeRequest(POST, basePath, jsonHeaders, jsonYearly)
      val respYearly = route(app, requestYearly).get

      status(respYearly) mustBe BAD_REQUEST
      val jsonResponseYearly = contentAsJson(respYearly)
      (jsonResponseYearly \ "code").get.toString should include("InvalidRequest")
      (jsonResponseYearly \ "msg").get.toString should include("max interval amount can not be less than or equal to 20.00 for yearly interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 1
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error when daily's and monthly's amount per interval is > yearly's" in {

      val jsonYearly =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "yearly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestYearly = FakeRequest(POST, basePath, jsonHeaders, jsonYearly)
      val respYearly = route(app, requestYearly).get

      val jsonResponseYearly = contentAsString(respYearly).as(classOf[LimitProfileToReadDetail]).toEither
      jsonResponseYearly.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(respYearly) mustBe CREATED

      val jsonMonthly =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestMonthly = FakeRequest(POST, basePath, jsonHeaders, jsonMonthly)
      val respMonthly = route(app, requestMonthly).get

      status(respMonthly) mustBe BAD_REQUEST
      val jsonResponseMonthly = contentAsJson(respMonthly)
      (jsonResponseMonthly \ "code").get.toString should include("InvalidRequest")
      (jsonResponseMonthly \ "msg").get.toString should include("max interval amount can not be more than or equal to 20.00 for monthly interval")


      val jsonDaily =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val requestDaily = FakeRequest(POST, basePath, jsonHeaders, jsonDaily)
      val respDaily = route(app, requestDaily).get

      status(respDaily) mustBe BAD_REQUEST
      val jsonResponseDaily = contentAsJson(respDaily)
      (jsonResponseDaily \ "code").get.toString should include("InvalidRequest")
      (jsonResponseDaily \ "msg").get.toString should include("max interval amount can not be more than or equal to 20.00 for daily interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 1
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return error on updating monthly limit profile interval amount if it's < daily" in {

      //create first daily
      val js1 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "daily",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 20.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req1 = FakeRequest(POST, basePath, jsonHeaders, js1)
      val resp1 = route(app, req1).get

      val jsResp1 = contentAsString(resp1).as(classOf[LimitProfileToReadDetail]).toEither
      val dailyId = jsResp1.map(_.id).right.get

      jsResp1.map(_.maxCountPerInterval) mustBe Right(Some(1))
      status(resp1) mustBe CREATED

      //create valid monthly
      val js2 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "monthly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req2 = FakeRequest(POST, basePath, jsonHeaders, js2)
      val resp2 = route(app, req2).get

      val jsResp2 = contentAsString(resp2).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp2) mustBe CREATED
      val monthlyId = jsResp2.map(_.id).right.get
      val updateAt2 = jsResp2.map(_.updatedAt).right.get

      //create valid yearly
      val js4 =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "yearly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 40.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": null,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req4 = FakeRequest(POST, basePath, jsonHeaders, js4)
      val resp4 = route(app, req4).get

      val jsResp4 = contentAsString(resp4).as(classOf[LimitProfileToReadDetail]).toEither
      status(resp4) mustBe CREATED
      val yearlyId = jsResp4.map(_.id).right.get

      //update monthly's interval to less than daily's should fail
      val ujs1 =
        s"""{
           |	"max_amount_per_interval": 5.00,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": 50,
           |	"max_count_per_interval": null,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req3 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs1)
      val resp3 = route(app, req3).get

      status(resp3) mustBe BAD_REQUEST
      val jsResp3 = contentAsJson(resp3)
      (jsResp3 \ "code").get.toString should include("InvalidRequest")
      (jsResp3 \ "msg").get.toString should include("max interval amount can not be less than or equal to 20.00 for monthly interval")

      //update monthly's interval to greater than yearly's should fail
      val ujs2 =
        s"""{
           |	"max_amount_per_interval": 50.00,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": 50,
           |	"max_count_per_interval": null,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req5 = FakeRequest(PUT, basePath + s"/$monthlyId", jsonHeaders, ujs2)
      val resp5 = route(app, req5).get

      status(resp5) mustBe BAD_REQUEST
      val jsResp5 = contentAsJson(resp5)
      (jsResp5 \ "code").get.toString should include("InvalidRequest")
      (jsResp5 \ "msg").get.toString should include("max interval amount can not be more than or equal to 40.00 for monthly interval")

      //update daily's interval to greatar than monthly's should fail
      val ujs3 =
        s"""{
           |	"max_amount_per_interval": 35.00,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": 50,
           |	"max_count_per_interval": null,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req6 = FakeRequest(PUT, basePath + s"/$dailyId", jsonHeaders, ujs3)
      val resp6 = route(app, req6).get

      status(resp6) mustBe BAD_REQUEST
      val jsResp6 = contentAsJson(resp6)
      (jsResp6 \ "code").get.toString should include("InvalidRequest")
      (jsResp6 \ "msg").get.toString should include("max interval amount can not be more than or equal to 30.00 for daily interval")

      //update yearly's interval to less than monthly's should fail
      val ujs4 =
        s"""{
           |	"max_amount_per_interval": 25.00,
           |	"min_amount_per_txn": null,
           |	"max_amount_per_txn": 50,
           |	"max_count_per_interval": null,
           |  "updated_at": ${updateAt2.fold("null")(t ⇒ s""""$t"""")}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val req7 = FakeRequest(PUT, basePath + s"/$yearlyId", jsonHeaders, ujs4)
      val resp7 = route(app, req7).get

      status(resp7) mustBe BAD_REQUEST
      val jsResp7 = contentAsJson(resp7)
      (jsResp7 \ "code").get.toString should include("InvalidRequest")
      (jsResp7 \ "msg").get.toString should include("max interval amount can not be less than or equal to 30.00 for yearly interval")

      val cleanResult = db.withConnection { implicit conn ⇒
        val idsToDelete: Seq[Int] = SQL(
          """
            |SELECT id FROM limit_profiles ORDER BY id DESC LIMIT 3
          """.stripMargin).executeQuery().as(SqlParser.scalar[Int].*)

        SQL(
          s"""
             |DELETE FROM limit_profile_history WHERE limit_profile_id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        val result = SQL(
          s"""
             |DELETE FROM limit_profiles WHERE id IN (${idsToDelete.mkString(", ")})
           """.stripMargin).executeUpdate()
        result > 0
      }
      cleanResult mustBe true
    }

    "return BAD_REQUEST on create limit profile when invalid interval" in {
      val jsonPayload =
        s"""{
           |	"limit_type": "transaction_based",
           |	"user_type": "individual",
           |	"tier": "tier_1",
           |	"subscription": "standard",
           |	"transaction_type": "currency_exchange",
           |	"channel": "mobile_application",
           |	"other_party": "Mashreq",
           |	"instrument": "debit_card",
           |	"interval": "weekly",
           |  "currency_code": "AED",
           |	"max_amount_per_interval": 30.00,
           |	"min_amount_per_txn": 5.0,
           |	"max_amount_per_txn": 50.00,
           |	"max_count_per_interval": 1,
           |  "max_balance_amount": null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      val request = FakeRequest(POST, basePath, jsonHeaders, jsonPayload)
      val resp = route(app, request).get

      //val jsonResponse = s"""{"id":"$mockRequestId","code":"InvalidRequest","msg":"provided element `weekly` is invalid "}""".stripMargin

      status(resp) mustBe BAD_REQUEST
      //contentAsString(resp) mustBe jsonResponse
      val json = contentAsJson(resp)
      (json \ "id").get mustBe JsString(mockRequestId.toString)
      (json \ "code").get mustBe JsString("InvalidRequest")
      (json \ "msg").get.toString().contains("provided element `weekly` is invalid") mustBe true
    }

  }
}
