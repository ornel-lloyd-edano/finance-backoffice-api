package tech.pegb.backoffice

import java.time.{Clock, Instant, LocalDate, ZoneId}
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, route, status, _}

class SavingFloatIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  private val savingFloatPath = "/api/floats"

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "Get floats API - Positive" should {
    "return list of floats for account for date range" in {
      val mockClockOne = Clock.fixed(Instant.ofEpochMilli(1545714782000L), ZoneId.systemDefault())
      val mockClockTwo = Clock.fixed(Instant.ofEpochMilli(1545805471000L), ZoneId.systemDefault())
      val currentDateStart = LocalDate.now(mockClockOne).atTime(0,0,0)
      val currentDateEnd = LocalDate.now(mockClockTwo).atTime(23,59,59)

      val resp = route(app, FakeRequest(GET, s"$savingFloatPath?date_from=$currentDateStart&date_to=$currentDateEnd&order_by=created_at").withHeaders(AuthHeader)).get

      val expected =
        s"""
           |[{
           |"user_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |"user_name": "Test",
           |"account_id": "734a4d3e-4327-45a4-96f6-e1eca9b4b442",
           |"account_number": "4.1",
           |"type": "standard_wallet",
           |"main_type": "liability",
           |"currency": "United Arab Emirates Dirham",
           |"internal_balance": 0.00,
           |"external_balance": null,
           |"inflow": 0.00,
           |"outflow": 1752.00,
           |"net": -1752.00,
           |"created_at": "2019-01-20T09:57:37Z",
           |"updated_at": "2019-01-20T09:57:37Z"}]
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) contains expected
    }

  }

  "Get floats API - Negative" should {
    "return bad request on invalid date range for floats" in {
      implicit val errorId: UUID = UUID.randomUUID()

      val mockClockOne = Clock.fixed(Instant.ofEpochMilli(1545805471000L), ZoneId.systemDefault())
      val mockClockTwo = Clock.fixed(Instant.ofEpochMilli(1545714782000L), ZoneId.systemDefault())
      val currentDateStart = LocalDate.now(mockClockOne).atTime(0,0,0)
      val currentDateEnd = LocalDate.now(mockClockTwo).atTime(23,59,59)

      val resp = route(app, FakeRequest(GET, s"$savingFloatPath?date_from=$currentDateStart&date_to=$currentDateEnd&order_by=created_at").withHeaders(AuthHeader)).get

      val expected =
        s"""
           {"id":"$errorId","code":"BadFormat","msg":"date_from must be before or equal to date_to"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains  expected
    }

    "return bad request on invalid order" in {
      implicit val errorId: UUID = UUID.randomUUID()

      val mockClockOne = Clock.fixed(Instant.ofEpochMilli(1545714782000L), ZoneId.systemDefault())
      val mockClockTwo = Clock.fixed(Instant.ofEpochMilli(1545805471000L), ZoneId.systemDefault())
      val currentDateStart = LocalDate.now(mockClockOne).atTime(0,0,0)
      val currentDateEnd = LocalDate.now(mockClockTwo).atTime(23,59,59)

      val resp = route(app, FakeRequest(GET, s"$savingFloatPath?date_from=$currentDateStart&date_to=$currentDateEnd&order_by=test").withHeaders(AuthHeader)).get

      val expected =
        s"""
           {"id":"$errorId","code":"BadFormat","msg":"date_from must be before or equal to date_to"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains  expected
    }

  }
}
