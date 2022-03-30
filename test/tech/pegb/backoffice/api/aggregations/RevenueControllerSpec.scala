package tech.pegb.backoffice.api.aggregations

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HttpVerbs
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class RevenueControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val httpClient = stub[HttpClient]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[HttpClient].to(httpClient),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val appConfig = inject[AppConfig]

  val aggregationURL = appConfig.AggregationEndpoints.aggregation
  val aggregationMarginURL = appConfig.AggregationEndpoints.aggregationMargin

  "RevenueControllerFacade " should {

    val turnoverAgg =
      s"""{
         |"results":[{
         |"aggregation":"turnover",
         |"amount":64500.00,
         |"currency_code":"KES",
         |"transaction_type":null,
         |"institution":null,
         |"timePeriod":null
         |}],
         |"total":1,
         |"limit":null,
         |"offset":null
         |}""".stripMargin

    val grossRevenueAgg =
      s"""{
         |"results":[{
         |"aggregation":"gross_revenue",
         |"amount":9650.00,
         |"currency_code":"KES",
         |"transaction_type":null,
         |"institution":null,
         |"time_period":null
         |}],
         |"total":1,
         |"limit":null,
         |"offset":null
         |}""".stripMargin

    val thirdPartyFeeAgg =
      s"""{
         |"results":[{
         |"aggregation":"third_party_fees",
         |"amount":2450.00,
         |"currency_code":"KES",
         |"transaction_type":null,
         |"institution":null,
         |"time_period":null
         |}],
         |"total":1,
         |"limit":null,
         |"offset":null
         |}""".stripMargin

    "get response of /api/aggregations and transform it to RevenueSummaryToRead" in {
      val turnoverTrendWithStep2 =
        s"""
           |{
           |  "total": 3,
           |  "results": [
           |    {
           |      "aggregation": "turnover",
           |      "amount": 3000,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "turnover",
           |      "amount": 11000,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "turnover",
           |      "amount": 16000,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-05"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val grossTrendWithStep2 =
        s"""
           |{
           |  "total": 3,
           |  "results": [
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1500,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1300,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1750,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-05"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val thirdPartyFeesTrendWithStep2 =
        s"""
           |{
           |  "total": 3,
           |  "results": [
           |    {
           |      "aggregation": "third_party_fees",
           |      "amount": 500,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "third_party_fees",
           |      "amount": 450,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "third_party_fees",
           |      "amount": 300,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-05"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val marginResponse =
        s"""
           |{
           |"results":[{
           |"margin": 0.038,
           |"currency_code": "KES",
           |"transaction_type": null,
           |"institution": null,
           |"time_period": null
           |}],
           |"limit": null,
           |"offset": null
           |}
         """.stripMargin

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=turnover&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59", None)
        .returns(Future.successful(HttpResponse(true, 200, turnoverAgg.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=turnover&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=time_period&step=${appConfig.AggregationConstants.step}", None)
        .returns(Future.successful(HttpResponse(true, 200, turnoverTrendWithStep2.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59", None)
        .returns(Future.successful(HttpResponse(true, 200, grossRevenueAgg.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=time_period&step=${appConfig.AggregationConstants.step}", None)
        .returns(Future.successful(HttpResponse(true, 200, grossTrendWithStep2.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59", None)
        .returns(Future.successful(HttpResponse(true, 200, thirdPartyFeeAgg.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=time_period&step=${appConfig.AggregationConstants.step}", None)
        .returns(Future.successful(HttpResponse(true, 200, thirdPartyFeesTrendWithStep2.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationMarginURL?currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59", None)
        .returns(Future.successful(HttpResponse(true, 200, marginResponse.some)))

      val resp = route(app, FakeRequest(GET, s"/revenue?currency_code=KES&date_from=2019-01-01&date_to=2019-01-06")
        .withHeaders(jsonHeaders)).get
      val expectedJson =
        s"""{
           |"turnover":{
           |"total_amount":64500.00,
           |"margin":[],
           |"data":[
           |{"time_period":"2019-01-01","amount":3000},
           |{"time_period":"2019-01-03","amount":11000},
           |{"time_period":"2019-01-05","amount":16000}
           |]},
           |"gross_revenue":{
           |"total_amount":9650.00,
           |"margin":[0.038],
           |"data":[
           |{"time_period":"2019-01-01","amount":1500},
           |{"time_period":"2019-01-03","amount":1300},
           |{"time_period":"2019-01-05","amount":1750}
           |]},
           |"third_party_fees":{
           |"total_amount":2450.00,
           |"margin":[],
           |"data":[
           |{"time_period":"2019-01-01","amount":500},
           |{"time_period":"2019-01-03","amount":450},
           |{"time_period":"2019-01-05","amount":300}
           |]}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
    "get response of /api/aggregations and transform it to RevenueAggregation" in {

      val grossTrend =
        s"""
           |{
           |  "total": 6,
           |  "results": [
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1500,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1600,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-02"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1300,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1700,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-04"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1750,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-05"
           |    },
           |    {
           |      "aggregation": "gross_revenue",
           |      "amount": 1800,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": "2019-01-06"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val marginResponse =
        s"""
           |{
           |"results":[{
           |"margin": 0.038,
           |"currency_code": "KES",
           |"transaction_type": null,
           |"institution": null,
           |"time_period": null
           |}],
           |"limit": null,
           |"offset": null
           |}
         """.stripMargin

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59", None)
        .returns(Future.successful(HttpResponse(true, 200, grossRevenueAgg.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=time_period", None)
        .returns(Future.successful(HttpResponse(true, 200, grossTrend.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationMarginURL?currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59", None)
        .returns(Future.successful(HttpResponse(true, 200, marginResponse.some)))

      val resp = route(app, FakeRequest(GET, s"/revenue/aggregation/gross_revenue?currency_code=KES&date_from=2019-01-01&date_to=2019-01-06")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{
           |"total_amount":9650.00,
           |"margin":[0.038],
           |"data":[
           |{"time_period":"2019-01-01","amount":1500},
           |{"time_period":"2019-01-02","amount":1600},
           |{"time_period":"2019-01-03","amount":1300},
           |{"time_period":"2019-01-04","amount":1700},
           |{"time_period":"2019-01-05","amount":1750},
           |{"time_period":"2019-01-06","amount":1800}
           |]}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
  }
  "get response of /api/aggregations and transform it to TransactionTotals" in {
    val turnoverByTxnType =
      s"""
         |{
         |  "total": 4,
         |  "results": [
         |    {
         |      "aggregation": "turnover",
         |      "amount": 18778.481012658227,
         |      "currency_code": "KES",
         |      "transaction_type": "P2P",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "turnover",
         |      "amount": 10341.772151898735,
         |      "currency_code": "KES",
         |      "transaction_type": "Remittance",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "turnover",
         |      "amount": 25582.278481012658,
         |      "currency_code": "KES",
         |      "transaction_type": "Split Bill",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "turnover",
         |      "amount": 9797.46835443038,
         |      "currency_code": "KES",
         |      "transaction_type": "Exchange",
         |      "institution": null,
         |      "time_period": null
         |    }
         |  ],
         |  "limit": null,
         |  "offset": null
         |}""".stripMargin

    val grossByTxnType =
      s"""
         |{
         |  "total": 4,
         |  "results": [
         |    {
         |      "aggregation": "gross_revenue",
         |      "amount": 1948.1220657276995,
         |      "currency_code": "KES",
         |      "transaction_type": "P2P",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "gross_revenue",
         |      "amount": 3986.8544600938967,
         |      "currency_code": "KES",
         |      "transaction_type": "Remittance",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "gross_revenue",
         |      "amount": 2491.7840375586857,
         |      "currency_code": "KES",
         |      "transaction_type": "Split Bill",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "gross_revenue",
         |      "amount": 1223.2394366197184,
         |      "currency_code": "KES",
         |      "transaction_type": "Exchange",
         |      "institution": null,
         |      "time_period": null
         |    }
         |  ],
         |  "limit": null,
         |  "offset": null
         |}
       """.stripMargin

    val thirdPartyByTxnType =
      s"""
         |{
         |  "total": 4,
         |  "results": [
         |    {
         |      "aggregation": "third_party_fees",
         |      "amount": 798,
         |      "currency_code": "KES",
         |      "transaction_type": "P2P",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "third_party_fees",
         |      "amount": 1092,
         |      "currency_code": "KES",
         |      "transaction_type": "Remittance",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "third_party_fees",
         |      "amount": 532,
         |      "currency_code": "KES",
         |      "transaction_type": "Split Bill",
         |      "institution": null,
         |      "time_period": null
         |    },
         |    {
         |      "aggregation": "third_party_fees",
         |      "amount": 28,
         |      "currency_code": "KES",
         |      "transaction_type": "Exchange",
         |      "institution": null,
         |      "time_period": null
         |    }
         |  ],
         |  "limit": null,
         |  "offset": null
         |}
       """.stripMargin

    (httpClient.request(_: String, _: String, _: Option[JsValue]))
      .when(HttpVerbs.GET, s"$aggregationURL?aggregation=turnover&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=transaction_type", None)
      .returns(Future.successful(HttpResponse(true, 200, turnoverByTxnType.some)))

    (httpClient.request(_: String, _: String, _: Option[JsValue]))
      .when(HttpVerbs.GET, s"$aggregationURL?aggregation=gross_revenue&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=transaction_type", None)
      .returns(Future.successful(HttpResponse(true, 200, grossByTxnType.some)))

    (httpClient.request(_: String, _: String, _: Option[JsValue]))
      .when(HttpVerbs.GET, s"$aggregationURL?aggregation=third_party_fees&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-06T23:59:59&group_by=transaction_type", None)
      .returns(Future.successful(HttpResponse(true, 200, thirdPartyByTxnType.some)))

    val resp = route(app, FakeRequest(GET, s"/revenue/transaction_totals?currency_code=KES&date_from=2019-01-01&date_to=2019-01-06")
      .withHeaders(jsonHeaders)).get

    val expectedJson =
      s"""
         |[{
         |"transaction_type":"Exchange",
         |"turnover":9797.46835443038,
         |"gross_revenue":1223.2394366197184,
         |"third_party_fees":28
         |},
         |{
         |"transaction_type":"P2P",
         |"turnover":18778.481012658227,
         |"gross_revenue":1948.1220657276995,
         |"third_party_fees":798
         |},
         |{
         |"transaction_type":"Remittance",
         |"turnover":10341.772151898735,
         |"gross_revenue":3986.8544600938967,
         |"third_party_fees":1092
         |},
         |{
         |"transaction_type":"Split Bill",
         |"turnover":25582.278481012658,
         |"gross_revenue":2491.7840375586857,
         |"third_party_fees":532
         |}
         |]""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

    status(resp) mustBe OK
    contentAsString(resp) mustBe expectedJson

  }
}
