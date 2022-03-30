package tech.pegb.backoffice

//import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, route, status, _}
import tech.pegb.backoffice.api.aggregations.controllers.CashFlowController


class CashFlowReportIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  override val mayBeDbName = Some("reports")
  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val cashFlowReportId = inject[CashFlowController].fixedReportId

  "Get CashFlow Dashboard API" should {
    "return cashflow aggregation" in {
      val resp = route(app, FakeRequest(GET, s"/api/reports/$cashFlowReportId/aggregations?currency=KES").withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |{"currency":"KES",
           |"total_bank_transfer":700.00,
           |"total_cash_in":700.00,
           |"total_cash_out":1450.00,
           |"total_txn_etc":2150.00}
           |""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return cashflow report" in {
      val resp = route(app, FakeRequest(GET, s"/api/reports/$cashFlowReportId").withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |{"total":7,
           |"results":[
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (343.2)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":0.00,
           |"cash_out":1250.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (66.25)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":500.00,
           |"cash_out":0.00,
           |"transactions":1250.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (airtel.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":0.00,
           |"cash_out":200.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (mpesa.2)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":200.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (pesalink.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":500.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-15T00:00Z",
           |"institution":"SBM (342.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":200.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-15T00:00Z",
           |"institution":"SBM (pesalink.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":200.00,
           |"closing_balance":0.00}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return cashflow report with pagination" in {
      val resp = route(app, FakeRequest(GET, s"/api/reports/$cashFlowReportId?limit=3&offset=3").withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |{"total":7,
           |"results":[
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (mpesa.2)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":200.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-17T00:00Z",
           |"institution":"SBM (pesalink.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":500.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-15T00:00Z",
           |"institution":"SBM (342.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":200.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00}],
           |"limit":3,
           |"offset":3}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }

    "return cashflow report with date range filter" in {
      val resp = route(app, FakeRequest(GET, s"/api/reports/$cashFlowReportId?date_from=2019-06-10&date_to=2019-06-15").withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |{"total":2,
           |"results":[
           |{"date":"2019-06-15T00:00Z",
           |"institution":"SBM (342.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":200.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":0.00,
           |"closing_balance":0.00},
           |{"date":"2019-06-15T00:00Z",
           |"institution":"SBM (pesalink.1)",
           |"currency":"KES",
           |"opening_balance":0.00,
           |"bank_transfer":0.00,
           |"cash_in":0.00,
           |"cash_out":0.00,
           |"transactions":200.00,
           |"closing_balance":0.00}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustEqual expected
    }
  }

}
