package tech.pegb.backoffice

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import anorm.{Row, SQL, SqlParser}
import cats.implicits._
import org.scalatest.Matchers._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.DBApi
import play.api.inject.{Binding, bind}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.dao.model.{Ordering, OrderingSet}
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.util.AppConfig

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

class ManualTransactionIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val httpClientService = stub[HttpClient]

  //TODO: remove binding when erland endpoint is ready
  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(bind[HttpClient].to(httpClientService))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val config = inject[AppConfig]

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
          defaultUserId = SQL("SELECT id FROM users WHERE username = '+971522106589' LIMIT 1;")
            .as(SqlParser.scalar[Int].single)
          defaultUserUuid = SQL("SELECT uuid FROM users WHERE id = {id} LIMIT 1;")
            .on("id" → defaultUserId)
            .as(SqlParser.scalar[UUID].single)
          defaultIndividualUserId = SQL("SELECT id FROM individual_users WHERE user_id = {user_id} LIMIT 1;")
            .on("user_id" → defaultUserId)
            .as(SqlParser.scalar[Int].single)
        }
      }
    } yield ())
      .leftMap(err ⇒ logger.error("Failed to prepare db: " + err))
    createSuperAdmin()
  }

  val accountNumberAed1: String = "1.6"
  val accountNumberAed2: String = "2.1"
  val accountNumberUsd1: String = "1.2"

  val accountIdAed1 = 22
  val accountIdAed2 = 6
  val accountIdUsd = 18

  val lastInsert = SQL("""SELECT AUTO_INCREMENT FROM information_schema.tables WHERE table_name = 'settlements' AND table_schema = DATABASE(); """)

  def getLastInsertedId: Int = {
    val dbApi = inject[DBApi]
    (for {
      db ← dbApi.database("backoffice").asRight[String]
    } yield {
      db.withTransaction{ implicit connection ⇒
        lastInsert.as(SqlParser.scalar[Int].single)
      }
    }).fold(
      left ⇒ 1,
      right ⇒ right
    )
  }

  "Manual Transaction API" should {
    val db = inject[DBApi].database("backoffice")

    "create normal manual_txn" in {
      val jsonRequest =
        s"""
           |{
           |"manual_txn_lines":[
           |{
           |"primary_direction":"DEBIT",
           |"primary_account_number":"$accountNumberAed1",
           |"primary_currency":"AED",
           |"secondary_account_number":"$accountNumberAed2",
           |"secondary_currency":"AED",
           |"amount":2000,
           |"primary_explanation":"Send money from 1.6 to 2.1",
           |"secondary_explanation":"Receive money from 1.6",
           |"secondary_amount":null
           |}],
           |"fx_details":null,
           |"transaction_reason":"manual txn test"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val coreRequest =
        s"""
           |[
           |    {
           |        "primary_account_id": $accountIdAed1,
           |        "primary_explanation": "Send money from 1.6 to 2.1",
           |        "primary_direction": "debit",
           |        "secondary_account_id": $accountIdAed2,
           |        "secondary_explanation": "Receive money from 1.6",
           |        "amount": 2000
           |    }
           |]
         """.stripMargin

      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))
      val nextId = getLastInsertedId

      (httpClientService.request(_: String, _: String, _: Option[JsValue]))
        .when("POST", s"${config.CreateManualTxnUrl.replaceAllLiterally("{id}",nextId.toString)}", Json.parse(coreRequest).some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      val resp = route(app, FakeRequest(POST, s"/api/manual_transactions", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CREATED
      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM settlements WHERE id = $nextId")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)

        val lines = SQL(s"SELECT * FROM settlement_lines WHERE settlement_id = $nextId")
        val foundLines: Seq[Row] = lines.executeQuery().as(lines.defaultParser.*)
        found.isDefined && foundLines.size == 2
      }
      isReallyInDB mustBe true
    }

    "create normal manual_txn fails when currency doesn't match account_id" in {
      val jsonRequest =
        s"""
           |{
           |"manual_txn_lines":[
           |{
           |"primary_direction":"DEBIT",
           |"primary_account_number":"$accountNumberAed1",
           |"primary_currency":"AED",
           |"secondary_account_number":"$accountNumberAed2",
           |"secondary_currency":"KES",
           |"amount":2000,
           |"primary_explanation":"Send money from 1.6 to 2.1",
           |"secondary_explanation":"Receive money from 1.6",
           |"secondary_amount":null
           |}],
           |"fx_details":null,
           |"transaction_reason":"manual txn test"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))
      val nextId = getLastInsertedId

      val resp = route(app, FakeRequest(POST, s"/api/manual_transactions", jsonHeaders, jsonRequest)).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("An account number doesnt match it's currency in the Transaction Lines")

    }

    "create fx manual_txn" in {
      val jsonRequest =
        s"""
           |{
           |"manual_txn_lines":[
           |{
           |"primary_direction":"DEBIT",
           |"primary_account_number":"$accountNumberUsd1",
           |"primary_currency":"USD",
           |"secondary_account_number":"$accountNumberAed1",
           |"secondary_currency":"AED",
           |"amount":200,
           |"primary_explanation":"DEBIT 200 USD from $accountNumberUsd1",
           |"secondary_explanation":"CREDIT 4000 AED to $accountNumberAed1",
           |"secondary_amount":4000
           |}],
           |"fx_details":{
           |"fx_provider":"BSP: Bank Sentral ng Pilipinas",
           |"fx_rate":20,
           |"from_currency":"USD",
           |"to_currency":"AED"
           |},
           |"transaction_reason":"manual txn test"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val coreRequest =
        s"""
           |[
           |    {
           |        "primary_account_id": $accountIdUsd,
           |        "primary_explanation": "DEBIT 200 USD from $accountNumberUsd1",
           |        "primary_amount": 200,
           |        "secondary_account_id": $accountIdAed1,
           |        "secondary_explanation": "CREDIT 4000 AED to $accountNumberAed1",
           |        "secondary_amount": 4000
           |    }
           |]
         """.stripMargin

      val nextId = getLastInsertedId

      (httpClientService.request(_: String, _: String, _: Option[JsValue]))
        .when("POST", s"${config.CreateManualTxnFxUrl.replaceAllLiterally("{id}",nextId.toString)}", Json.parse(coreRequest).some)
        .returns(Future.successful(HttpResponse(true, 204, None)))

      val resp = route(app, FakeRequest(POST, s"/api/manual_transactions", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CREATED
      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM settlements WHERE id = $nextId")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)

        val lines = SQL(s"SELECT * FROM settlement_lines WHERE settlement_id = $nextId")
        val foundLines: Seq[Row] = lines.executeQuery().as(lines.defaultParser.*)
        found.isDefined && foundLines.size == 2
      }
      isReallyInDB mustBe true
    }

    "create fx manual_txn fails when currency doesn't match account_id" in {
      val jsonRequest =
        s"""
           |{
           |"manual_txn_lines":[
           |{
           |"primary_direction":"DEBIT",
           |"primary_account_number":"$accountNumberUsd1",
           |"primary_currency":"USD",
           |"secondary_account_number":"$accountNumberAed1",
           |"secondary_currency":"KES",
           |"amount":200,
           |"primary_explanation":"DEBIT 200 USD from $accountNumberUsd1",
           |"secondary_explanation":"CREDIT 4000 AED to $accountNumberAed1",
           |"secondary_amount":4000
           |}],
           |"fx_details":{
           |"fx_provider":"BSP: Bank Sentral ng Pilipinas",
           |"fx_rate":20,
           |"from_currency":"USD",
           |"to_currency":"KES"
           |},
           |"transaction_reason":"manual txn test"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))
      val nextId = getLastInsertedId

      val resp = route(app, FakeRequest(POST, s"/api/manual_transactions", jsonHeaders, jsonRequest)).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("An account number doesnt match it's currency in the Transaction Lines")

    }


    "create fx manual_txn fails when currency doesn't match fx_details" in {
      val jsonRequest =
        s"""
           |{
           |"manual_txn_lines":[
           |{
           |"primary_direction":"DEBIT",
           |"primary_account_number":"$accountNumberUsd1",
           |"primary_currency":"KES",
           |"secondary_account_number":"$accountNumberAed1",
           |"secondary_currency":"AED",
           |"amount":200,
           |"primary_explanation":"DEBIT 200 USD from $accountNumberUsd1",
           |"secondary_explanation":"CREDIT 4000 AED to $accountNumberAed1",
           |"secondary_amount":4000
           |}],
           |"fx_details":{
           |"fx_provider":"BSP: Bank Sentral ng Pilipinas",
           |"fx_rate":20,
           |"from_currency":"USD",
           |"to_currency":"AED"
           |},
           |"transaction_reason":"manual txn test"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))
      val nextId = getLastInsertedId

      val resp = route(app, FakeRequest(POST, s"/api/manual_transactions", jsonHeaders, jsonRequest)).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("primary_currency == from_currency AND secondary_currency == to_currency for all TransactionLines")

    }

    "GET fx history" in {
      val resp = route(app, FakeRequest(GET, s"/manual_transactions/currency_exchange_history")
        .withHeaders(jsonHeaders)).get

      val jsonResponse =
        s"""
           |"fx_provider":"BSP: Bank Sentral ng Pilipinas",
           |"from_currency":"USD",
           |"from_flag":"usd_icon",
           |"to_currency":"AED",
           |"to_flag":"aed_icon",
           |"fx_rate":20.000000,""".stripMargin.trim.replace(System.lineSeparator(), "")

      contentAsString(resp) should include (jsonResponse)
      status(resp) mustBe OK
    }

  }
}
