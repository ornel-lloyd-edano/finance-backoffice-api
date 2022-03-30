package tech.pegb.backoffice

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}

class RevenueAndFloatDashboardIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  override val mayBeDbName = Some("reports")

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
    "Get Revenue Dashboard API - Positive" should {

      "return all revenues " in {
        val resp = route(app, FakeRequest(GET, s"/revenue?currency_code=KES")).get

        val expected =
          s"""
             |{
             |"turnover": {
             |"total_amount": 2150.00,
             |"margin": [],
             |"data": [{
             |"time_period": "2019-06-15",
             |"amount": 200.00
             |},
             |{
             |"time_period": "2019-06-17",
             |"amount": 1950.00
             |}
             |]
             |},
             |"gross_revenue": {
             |"total_amount": 0.0,
             |"margin": [0.000],
             |"data": [{
             |"time_period": "2019-06-15",
             |"amount": 0.0
             |},
             |{
             |"time_period": "2019-06-17",
             |"amount": 0.0
             |}
             |]
             |},
             |"third_party_fees": {
             |"total_amount": 0.00,
             |"margin": [],
             |"data": [{
             |"time_period": "2019-06-15",
             |"amount": 0.00
             |}, {
             |"time_period": "2019-06-17",
             |"amount": 0.00
             |}]
             |}
             |}
           """.stripMargin.trim.replaceAll(" ","").replaceAll(System.lineSeparator(), "")

        status(resp) mustBe OK

       contentAsString(resp) mustEqual expected
      }

      "return revenues aggregation by aggregation type" in {
        val resp = route(app, FakeRequest(GET, s"/revenue/aggregation/turnover?currency_code=KES")).get

        val expected =
          s"""{"total_amount":2150.00,"margin":[],
             |"data":[{"time_period":"2019-06-15","amount":200.00},
             |{"time_period":"2019-06-17","amount":1950.00}]}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")


        status(resp) mustBe OK

        contentAsString(resp) mustEqual expected
      }

      "return revenue transaction totals" in {
        val resp = route(app, FakeRequest(GET, s"/revenue/transaction_totals?currency_code=KES")).get

        val expected =
          s"""
             |[{
             |"transaction_type": "bank_transfer",
             |"turnover": 700.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}, {
             |"transaction_type": "cashin",
             |"turnover": 700.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}, {
             |"transaction_type": "cashout",
             |"turnover": 1450.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}, {
             |"transaction_type": "p2p_domestic",
             |"turnover": 1450.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}]
           """.stripMargin.trim.replaceAll(" ","").replaceAll(System.lineSeparator(), "")

        status(resp) mustBe OK
        contentAsString(resp) contains expected
      }
    }

    "Get Revenue Dashboard API - Negative" should {
      "fail to return all revenues if no currency code is provided"  in {
        val resp = route(app, FakeRequest(GET, s"/revenue")).get

        val expected = """"code":"Unknown","msg":"Missing parameter: currency_code""""

        status(resp) mustBe BAD_REQUEST

        contentAsString(resp) contains  expected
      }

      "fail to return revenues if invalid parameter is provided provided" in {
        val resp = route(app, FakeRequest(GET, s"/revenue/aggregation/121121?currency=KES")).get

        status(resp) mustBe BAD_REQUEST

      }

      "return revenue transaction totals" in {
        val resp = route(app, FakeRequest(GET, s"/revenue/transaction_totals?currency_code=KES")).get

        val expected =
          s"""
             |[{
             |"transaction_type": "cashout",
             |"turnover": 1450.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}, {
             |"transaction_type": "p2p_domestic",
             |"turnover": 1450.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}, {
             |"transaction_type": "bank_transfer",
             |"turnover": 700.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}, {
             |"transaction_type": "cashin",
             |"turnover": 700.0000,
             |"gross_revenue": 0.0,
             |"third_party_fees": 0.0
             |}]
           """.stripMargin.trim.replaceAll(" ","").replaceAll(System.lineSeparator(), "")

        status(resp) mustBe OK

        contentAsString(resp) contains expected
      }
    }

  "Get Float Dashboard API - Positive" should {
    "return float totals " in {
      val resp = route(app, FakeRequest(GET, s"/floats/totals?currency_code=KES")).get

      val expected =
        s"""
           |{
           |"institution_collection_balance":810926.3200,
           |"institution_distribution_balance":1399269.9300,
           |"user_balance":0.0,
           |"pending_balance":0}""".stripMargin.trim.replaceAll(" ","").replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expected
    }
    
    "return float totals for institutions" in {
      val resp = route(app, FakeRequest(GET, s"/floats/institutions?currency_code=KES")).get

      val expected =
        s"""
           |[{
           |"name":"SBM",
           |"distribution_account_balance":100.0000,
           |"institution_user_balance_percentage":100,
           |"calculated_user_balance":0.0,
           |"pending_balance":0}]""".stripMargin.trim.replaceAll(" ","").replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustEqual expected
    }

    "return revenue transaction totals" in {
      val resp = route(app, FakeRequest(GET, s"/revenue/transaction_totals?currency_code=KES")).get

      val expected =
        s""""
           |[{
           |"transaction_type": "bank_transfer",
           |"turnover": 700.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "cashin",
           |"turnover": 700.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "cashout",
           |"turnover": 1450.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "p2p_domestic",
           |"turnover": 1450.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}]"""".stripMargin.replaceAll(" ","").trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

       contentAsString(resp) contains expected
    }

    "return revenue transaction totals based on txn created date" in {
      val resp = route(app, FakeRequest(GET, s"/revenue/transaction_totals?currency_code=KES&date_from=2020-01-01&date_to=2020-01-30")).get

      val expected =
        s""""
           |[{
           |"transaction_type": "bank_transfer",
           |"turnover": 700.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "cashin",
           |"turnover": 700.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "cashout",
           |"turnover": 1450.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "p2p_domestic",
           |"turnover": 1450.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}]"""".stripMargin.replaceAll(" ","").trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) contains expected
    }

    "return third party fees based on txn created date " in {
      val resp = route(app, FakeRequest(GET, s"/api/aggregations?aggregation=third_party_fees&currency_code=KES&date_from=2020-01-21T00:00&date_to=2020-01-27T23:59:59&group_by=time_period")).get

      val expected =
        s""""
           |[{
           |"transaction_type": "bank_transfer",
           |"turnover": 700.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "cashin",
           |"turnover": 700.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "cashout",
           |"turnover": 1450.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}, {
           |"transaction_type": "p2p_domestic",
           |"turnover": 1450.0000,
           |"gross_revenue": 0.0,
           |"third_party_fees": 0.0
           |}]"""".stripMargin.replaceAll(" ","").trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) contains expected
    }
    "return balance based on currency code " in {
      val resp = route(app, FakeRequest(GET, s"/api/aggregations?aggregation=account_balance&currency_code=KES&account_type=distribution")).get

      val expected =
        s""""
           |[{"total":1,
           |"results":[{"aggregation":"balance","amount":0.0,"currency_code":"unknown","transaction_type":null,"institution":null,"time_period":null}],"limit":null,"offset":null}
           |]"""".stripMargin.replaceAll(" ","").trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) contains expected
    }

    //api/aggregations?aggregation=third_party_fees&currency_code=KES&date_from=2020-01-20T00:00&date_to=2020-01-27T23:59:59&group_by=time_period&step=1
    //GET http://192.168.35.92:8601/api/aggregations?aggregation=balance&currency_code=KES&account_type=distribution&group_by=institution


  }
}
