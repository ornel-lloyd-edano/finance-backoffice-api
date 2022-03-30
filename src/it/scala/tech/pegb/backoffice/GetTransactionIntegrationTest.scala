package tech.pegb.backoffice

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import anorm.{SQL, SqlParser}
import cats.implicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.DBApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.util.Implicits._

import scala.io.Source
import scala.util.Try

class GetTransactionIntegrationTest extends PlayIntegrationTest with ScalaFutures {
  private val baseTransactionsPath = "/api/customers"

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private var accountId1: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    val dbApi = inject[DBApi]
    logger.info(s"Databases are:\n${dbApi.databases().map(db ⇒ s"${db.name}: ${db.url}").mkString("\n")}")
    (for {
      rawDropSql ← Try(Source.fromResource(dropSchemaSqlName).mkString).toEither.leftMap(_.getMessage)
      rawDataSql ← Try(Source.fromResource(initDataSqlName).mkString).toEither.leftMap(_.getMessage)
      db ← dbApi.database("backoffice").asRight[String]
      _ ← db.withTransaction { implicit connection ⇒
        for {
          _ ← runUpdateSql(rawDropSql)
          _ ← runUpdateSql(rawDataSql)
        } yield {
          defaultUserId  = SQL("SELECT id FROM users WHERE username = '+971522106589' LIMIT 1;")
          .as(SqlParser.scalar[Int].single)
          defaultUserUuid  = SQL("SELECT uuid FROM users WHERE id = {id} LIMIT 1;")
          .on("id" → defaultUserId)
          .as(SqlParser.scalar[UUID].single)
                    defaultIndividualUserId = SQL("SELECT id FROM individual_users WHERE user_id = {user_id} LIMIT 1;")
          .on("user_id" → defaultUserId)
          .as(SqlParser.scalar[Int].single)

          accountId1 = SQL("SELECT uuid FROM accounts WHERE user_id = {user_id} LIMIT 1;")
          .on("user_id" → defaultUserId)
          .as(SqlParser.scalar[UUID].single)
        }
      }
    } yield createSuperAdmin())
      .leftMap(err ⇒ logger.error("Failed to prepare db: " + err))
  }

  "Get Transaction API - POSITIVE" should {
    "return list ordered by created_at DESC, sequence ASC as default sorter" in {
      val request = FakeRequest(GET, s"/api/customers/$defaultUserUuid/accounts/$accountId1/transactions").withHeaders(AuthHeader)

      val expected =
        s"""
           |{
           |"total":5,
           |"results":[
           |{"id":"1549446333",
           |"sequence":1,
           |"primary_account_id":"$accountId1",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273be701-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_fee_collection",
           |"secondary_account_number":"pegb_fees",
           |"direction":"debit",
           |"type":"merchant_payment",
           |"amount":500.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018,12,26,3,7,30).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{"id":"1549446333",
           |"sequence":3,
           |"primary_account_id":"${accountId1}",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"4cf577ec-d410-49b1-843d-4ba3509a11b7",
           |"secondary_account_name":"+971544451674_standard_wallet",
           |"secondary_account_number":"3.1",
           |"direction":"debit",
           |"type":"fee",
           |"amount":1.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"fee",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018,12,26,3,7,30).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{
           |"id":"1549446999",
           |"sequence":2,
           |"primary_account_id":"734a4d3e-4327-45a4-96f6-e1eca9b4b442",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273be701-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_fee_collection",
           |"secondary_account_number":"pegb_fees",
           |"direction":"credit",
           |"type":"p2p_domestic",
           |"amount":200.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"ANDROID_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 14, 27, 30).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018, 12, 27, 0, 0, 0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{"id":"1549449579",
           |"sequence":1,
           |"primary_account_id":"734a4d3e-4327-45a4-96f6-e1eca9b4b442",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273bd2d6-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_utility",
           |"secondary_account_number":"pegb_vouchers",
           |"direction":"debit","type":"p2p_domestic",
           |"amount":1250.00,"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 0, 0, 0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{
           |"id":"1549449579",
           |"sequence":3,
           |"primary_account_id":"734a4d3e-4327-45a4-96f6-e1eca9b4b442",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"4cf577ec-d410-49b1-843d-4ba3509a11b7",
           |"secondary_account_name":"+971544451674_standard_wallet",
           |"secondary_account_number":"3.1",
           |"direction":"debit",
           |"type":"fee",
           |"amount":1.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"fee",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 0, 0, 0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |}
           |],
           |"limit":null,
           |"offset":null
           |}
           |""".stripMargin.replace(System.lineSeparator(), "")
      val resp = route(app, request).get


      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }


    "getRequest test no limit - should use default limit, and return total as minimum(real count, max cap)" in {
      val request = FakeRequest(GET, s"/api/customers/$defaultUserUuid/accounts/$accountId1/transactions?&order_by=sequence,created_at").withHeaders(AuthHeader)

      val expected =
        s"""
           |{
           |"total":5,
           |"results":[
           |{"id":"1549449579",
           |"sequence":1,
           |"primary_account_id":"${accountId1}",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273bd2d6-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_utility",
           |"secondary_account_number":"pegb_vouchers",
           |"direction":"debit",
           |"type":"p2p_domestic",
           |"amount":1250.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 0, 0, 0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{"id":"1549446333",
           |"sequence":1,
           |"primary_account_id":"${accountId1}",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273be701-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_fee_collection",
           |"secondary_account_number":"pegb_fees",
           |"direction":"debit",
           |"type":"merchant_payment",
           |"amount":500.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018,12,26,3,7,30).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{
           |"id":"1549446999",
           |"sequence":2,
           |"primary_account_id":"734a4d3e-4327-45a4-96f6-e1eca9b4b442",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273be701-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_fee_collection",
           |"secondary_account_number":"pegb_fees",
           |"direction":"credit",
           |"type":"p2p_domestic",
           |"amount":200.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"ANDROID_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 14, 27, 30).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{"id":"1549449579",
           |"sequence":3,
           |"primary_account_id":"${accountId1}",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"4cf577ec-d410-49b1-843d-4ba3509a11b7",
           |"secondary_account_name":"+971544451674_standard_wallet",
           |"secondary_account_number":"3.1",
           |"direction":"debit",
           |"type":"fee",
           |"amount":1.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"fee",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 0, 0, 0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |},
           |{"id":"1549446333",
           |"sequence":3,
           |"primary_account_id":"${accountId1}",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"4cf577ec-d410-49b1-843d-4ba3509a11b7",
           |"secondary_account_name":"+971544451674_standard_wallet",
           |"secondary_account_number":"3.1",
           |"direction":"debit",
           |"type":"fee",
           |"amount":1.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"fee",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018,12,26,3,7,30).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replace(System.lineSeparator(), "")
      val resp = route(app, request).get

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "get transactions request with all filter " in {
      val url = s"/api/customers/$defaultUserUuid/accounts/$accountId1/transactions?" +
        s"date_from=2018-12-25&" +
        s"date_to=2018-12-26&" +
        s"type=p2p_domestic&" +
        s"channel=IOS_APP&" +
        s"order_by=-created_at,direction"
      val request = FakeRequest(GET, url).withHeaders(AuthHeader)

      val resp = route(app, request).get

      val expected =
        s"""
           |{"total":1,
           |"results":[
           |{"id":"1549449579",
           |"sequence":1,
           |"primary_account_id":"${accountId1}",
           |"primary_account_name":"+971522106589_standard_wallet",
           |"primary_account_number":"4.1",
           |"primary_account_customer_name":"Test",
           |"secondary_account_id":"273bd2d6-1c83-11e9-a2a9-000c297e3e45",
           |"secondary_account_name":"1_utility",
           |"secondary_account_number":"pegb_vouchers",
           |"direction":"debit",
           |"type":"p2p_domestic",
           |"amount":1250.00,
           |"currency":"AED",
           |"exchange_currency":null,
           |"channel":"IOS_APP",
           |"explanation":"some explanation",
           |"effective_rate":null,
           |"cost_rate":null,
           |"status":"success",
           |"created_at":"${LocalDateTime.of(2018, 12, 25, 0, 0, 0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"updated_at":"${LocalDateTime.of(2018,12,27,0,0,0).toZonedDateTimeUTC.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}",
           |"previous_balance":1000.00,
           |"reason":null
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected

    }
  }

  "Get Transaction API - NEGATIVE" should {
    "getRequest test user not found" in {
      val fakeUser = UUID.randomUUID()
      val requestId = UUID.randomUUID()
      val request = FakeRequest(GET, s"/api/customers/$fakeUser/accounts/$accountId1/transactions?date_from=2018-12-25&date_to=2018-12-26&order_by=-created_at,direction")
        .withHeaders(Seq(("request-id",requestId.toString), AuthHeader): _*)

      val expected = """{"total":0,"results":[],"limit":null,"offset":null}"""
      val resp = route(app, request).get
      val content = contentAsString(resp)

      content mustBe expected
    }
    "getRequest account not found" in {
      val fakeAccount = UUID.randomUUID()
      val requestId = UUID.randomUUID()
      val request = FakeRequest(GET, s"/api/customers/$defaultUserUuid/accounts/$fakeAccount/transactions?date_from=2018-12-25&date_to=2018-12-26&order_by=-created_at,direction")
        .withHeaders(Seq(("request-id",requestId.toString), AuthHeader): _*)

      val expected = """{"total":0,"results":[],"limit":null,"offset":null}"""
      val resp = route(app, request).get
      val content = contentAsString(resp)

      content mustBe expected
    }
    "getRequest date_from is after date_to" in {
      val fakeAccount = UUID.randomUUID()
      val requestId = UUID.randomUUID()
      val request = FakeRequest(GET, s"/api/customers/$defaultUserUuid/accounts/$fakeAccount/transactions?date_from=2018-12-30&date_to=2018-12-26&order_by=-created_at,direction")
        .withHeaders(Seq(("request-id",requestId.toString), AuthHeader): _*)

      val resp = route(app, request).get
      status(resp) mustBe BAD_REQUEST
    }
  }
}