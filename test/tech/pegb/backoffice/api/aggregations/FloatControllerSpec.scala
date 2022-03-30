package tech.pegb.backoffice.api.aggregations

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HttpVerbs
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class FloatControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

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

  val parameterEndpoint = s"${appConfig.Hosts.MainBackofficeApi}/parameters"
  val floatUserBalancePercentageParameterUrl = s"$parameterEndpoint?key=${appConfig.Aggregations.floatUserBalancePercentageKey}"

  "FloatControllerFacade " should {
    "get response of /api/aggregation and transform to FloatTotals " in {
      val collectionAgg =
        s"""
           |{
           |  "total": 1,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 203521,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": null
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val distributionAgg =
        s"""
           |{
           |  "total": 1,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 183937.59,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": null
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val endUserAgg =
        s"""
           |{
           |  "total": 1,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 12252,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": null
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=account_balance&currency_code=KES&account_type=collection", None)
        .returns(Future.successful(HttpResponse(true, 200, collectionAgg.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=account_balance&currency_code=KES&account_type=distribution", None)
        .returns(Future.successful(HttpResponse(true, 200, distributionAgg.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=account_balance&currency_code=KES&account_type=standard_saving,standard_wallet", None)
        .returns(Future.successful(HttpResponse(true, 200, endUserAgg.some)))

      val resp = route(app, FakeRequest(GET, s"/floats/totals?currency_code=KES&date_from=2019-01-01&date_to=2019-01-06")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{"institution_collection_balance":203521,
           |"institution_distribution_balance":183937.59,
           |"user_balance":12252,
           |"pending_balance":0}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
    "get response of /api/aggregation and transform to InstitutionTrendGraph in /floats/insititution/:name/trends frequency = daily" in {
      val cashin =
        s"""
           |{
           |  "total": 7,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 1434,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 7078,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-02"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 1772,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 19344,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-04"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 12322,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-05"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 1085,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-06"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 14352,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_in",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-07"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val cashout =
        s"""
           |{
           |  "total": 7,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 7128,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 16740,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-02"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 15768,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 17059,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-04"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 8939,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-05"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 2598,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-06"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 51,
           |      "currency_code": "KES",
           |      "transaction_type": "cash_out",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-07"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val etcTxn =
        s"""
           |{
           |  "total": 7,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 19044,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-01"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 756,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-02"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 3074,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-03"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 5512,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-04"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 15084,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-05"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 8238,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-06"
           |    },
           |    {
           |      "aggregation": "balance",
           |      "amount": 13173,
           |      "currency_code": "KES",
           |      "transaction_type": "etc_transactions",
           |      "institution": "mPesa",
           |      "time_period": "2019-01-07"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val closingUserBalance =
        s"""
           |{
           |  "total": 0,
           |  "results": [],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=amount&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-07T23:59:59&transaction_type=cashin&institution=mPesa&frequency=daily&group_by=institution,transaction_type,time_period&user_type=provider", None)
        .returns(Future.successful(HttpResponse(true, 200, cashin.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=amount&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-07T23:59:59&transaction_type=cashout&institution=mPesa&frequency=daily&group_by=institution,transaction_type,time_period&user_type=provider", None)
        .returns(Future.successful(HttpResponse(true, 200, cashout.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=amount&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-07T23:59:59&transaction_type=etc_transactions&institution=mPesa&frequency=daily&group_by=institution,time_period&user_type=provider", None)
        .returns(Future.successful(HttpResponse(true, 200, etcTxn.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=balance&currency_code=KES&date_from=2019-01-01T00:00&date_to=2019-01-07T23:59:59&institution=mPesa&frequency=daily&group_by=institution,time_period&user_type=${appConfig.Aggregations.closingUserBalanceUserType}", None)
        .returns(Future.successful(HttpResponse(true, 200, closingUserBalance.some)))

      val resp = route(app, FakeRequest(GET, s"/floats/institutions/mPesa/trends?currency_code=KES&date_from=2019-01-01&date_to=2019-01-07&frequency=daily")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{"cash_in":[
           |{"time_period":"2019-01-01","amount":1434},
           |{"time_period":"2019-01-02","amount":7078},
           |{"time_period":"2019-01-03","amount":1772},
           |{"time_period":"2019-01-04","amount":19344},
           |{"time_period":"2019-01-05","amount":12322},
           |{"time_period":"2019-01-06","amount":1085},
           |{"time_period":"2019-01-07","amount":14352}
           |],
           |"transactions":[
           |{"time_period":"2019-01-01","amount":19044},
           |{"time_period":"2019-01-02","amount":756},
           |{"time_period":"2019-01-03","amount":3074},
           |{"time_period":"2019-01-04","amount":5512},
           |{"time_period":"2019-01-05","amount":15084},
           |{"time_period":"2019-01-06","amount":8238},
           |{"time_period":"2019-01-07","amount":13173}
           |],
           |"cash_out":[
           |{"time_period":"2019-01-01","amount":7128},
           |{"time_period":"2019-01-02","amount":16740},
           |{"time_period":"2019-01-03","amount":15768},
           |{"time_period":"2019-01-04","amount":17059},
           |{"time_period":"2019-01-05","amount":8939},
           |{"time_period":"2019-01-06","amount":2598},
           |{"time_period":"2019-01-07","amount":51}
           |],
           |"closing_user_balance":[]}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
    "get response of /api/aggregation and transform to InstitutionTrendGraph in /floats/insititutions frequency = daily" in {
      val distribution =
        """
          |{
          |  "total": 3,
          |  "results": [
          |    {
          |      "aggregation": "balance",
          |      "amount": 73583.04,
          |      "currency_code": "KES",
          |      "transaction_type": null,
          |      "institution": "SBM",
          |      "time_period": null
          |    },
          |    {
          |      "aggregation": "balance",
          |      "amount": 63422.12,
          |      "currency_code": "KES",
          |      "transaction_type": null,
          |      "institution": "KCB",
          |      "time_period": null
          |    },
          |    {
          |      "aggregation": "balance",
          |      "amount": 46932.43,
          |      "currency_code": "KES",
          |      "transaction_type": null,
          |      "institution": "mPesa",
          |      "time_period": null
          |    }
          |  ],
          |  "limit": null,
          |  "offset": null
          |}
        """.stripMargin

      val enduser =
        s"""
           |{
           |  "total": 1,
           |  "results": [
           |    {
           |      "aggregation": "balance",
           |      "amount": 12252,
           |      "currency_code": "KES",
           |      "transaction_type": null,
           |      "institution": null,
           |      "time_period": null
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val parameter =
        s"""
           |{
           |  "total": 1,
           |  "results": [
           |    {
           |      "id": "30303033-3a30-3030-3030-303030303337",
           |      "key": "float_user_balance_percentage_institution_map",
           |      "value": {
           |        "id": 37,
           |        "key": "float_user_balance_percentage_institution_map",
           |        "value": "[{\\"name\\":\\"KCB\\",\\"percentage\\":50},{\\"name\\":\\"mPesa\\",\\"percentage\\":80}]",
           |        "type": "json",
           |        "explanation": "List of mapping between institution and percentage of total user balance for float dashboard",
           |        "forAndroid": false,
           |        "forIOS": false,
           |        "forBackoffice": true,
           |        "createdAt": "2019-12-24T07:17:18",
           |        "createdBy": "Narek",
           |        "updatedAt": "2019-12-30T08:24:59",
           |        "updatedBy": "UNKNOWNUSER"
           |      },
           |      "platforms": [
           |        "BACKOFFICE"
           |      ],
           |      "metadata_id": "system_settings",
           |      "explanation": "List of mapping between institution and percentage of total user balance for float dashboard",
           |      "created_at": "2019-12-24T07:17:18Z",
           |      "created_by": "Narek",
           |      "updated_at": "2019-12-30T08:24:59Z",
           |      "updated_by": "UNKNOWNUSER"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=balance&currency_code=KES&account_type=distribution&group_by=institution", None)
        .returns(Future.successful(HttpResponse(true, 200, distribution.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, s"$aggregationURL?aggregation=account_balance&currency_code=KES&account_type=standard_saving,standard_wallet", None)
        .returns(Future.successful(HttpResponse(true, 200, enduser.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, floatUserBalancePercentageParameterUrl, None)
        .returns(Future.successful(HttpResponse(true, 200, parameter.some)))

      val resp = route(app, FakeRequest(GET, s"/floats/institutions?currency_code=KES")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |[
           |{
           |"name":"SBM",
           |"distribution_account_balance":73583.04,
           |"institution_user_balance_percentage":100,
           |"calculated_user_balance":12252,
           |"pending_balance":0
           |},
           |{
           |"name":"KCB",
           |"distribution_account_balance":63422.12,
           |"institution_user_balance_percentage":50,
           |"calculated_user_balance":6126,
           |"pending_balance":0
           |},
           |{
           |"name":"mPesa",
           |"distribution_account_balance":46932.43,
           |"institution_user_balance_percentage":80,
           |"calculated_user_balance":9801.6,
           |"pending_balance":14700
           |}]""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "update parameter float_user_balance_percentage_institution_map via PUT /floats/institutitions" in {
      val parameter =
        s"""
           |{
           |  "total": 1,
           |  "results": [
           |    {
           |      "id": "30303033-3a30-3030-3030-303030303337",
           |      "key": "float_user_balance_percentage_institution_map",
           |      "value": {
           |        "id": 37,
           |        "key": "float_user_balance_percentage_institution_map",
           |        "value": "[{\\"name\\":\\"KCB\\",\\"percentage\\":50},{\\"name\\":\\"mPesa\\",\\"percentage\\":80}]",
           |        "type": "json",
           |        "explanation": "List of mapping between institution and percentage of total user balance for float dashboard",
           |        "forAndroid": false,
           |        "forIOS": false,
           |        "forBackoffice": true,
           |        "createdAt": "2019-12-24T07:17:18",
           |        "createdBy": "Narek",
           |        "updatedAt": "2019-12-30T08:24:59",
           |        "updatedBy": "UNKNOWNUSER"
           |      },
           |      "platforms": [
           |        "BACKOFFICE"
           |      ],
           |      "metadata_id": "system_settings",
           |      "explanation": "List of mapping between institution and percentage of total user balance for float dashboard",
           |      "created_at": "2019-12-24T07:17:18Z",
           |      "created_by": "Narek",
           |      "updated_at": "2019-12-30T08:24:59Z",
           |      "updated_by": "UNKNOWNUSER"
           |    }
           |  ],
           |  "limit": null,
           |  "offset": null
           |}
         """.stripMargin

      val updateValue =
        s"""
           |{
           |"value": {
           |  "id": 37,
           |  "key": "float_user_balance_percentage_institution_map",
           |  "value": "[{\\"name\\":\\"SBM\\",\\"percentage\\":100},{\\"name\\":\\"KCB\\",\\"percentage\\":50},{\\"name\\":\\"mPesa\\",\\"percentage\\":80}]",
           |  "type": "json",
           |  "explanation": "List of mapping between institution and percentage of total user balance for float dashboard",
           |  "forAndroid": false,
           |  "forIOS": false,
           |  "forBackoffice": true,
           |  "createdAt": "2019-12-24T07:17:18",
           |  "createdBy": "Narek",
           |  "updatedAt": "2019-12-30T08:24:59",
           |  "updatedBy": "UNKNOWNUSER"
           |},
           |"platforms": [
           |  "BACKOFFICE"
           |],
           |"metadata_id": "system_settings",
           |"explanation": "List of mapping between institution and percentage of total user balance for float dashboard",
           |"updated_at": "2019-12-30T08:24:59Z"
           |}
         """.stripMargin

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.GET, floatUserBalancePercentageParameterUrl, None)
        .returns(Future.successful(HttpResponse(true, 200, parameter.some)))

      (httpClient.request(_: String, _: String, _: Option[JsValue]))
        .when(HttpVerbs.PUT, s"$parameterEndpoint/30303033-3a30-3030-3030-303030303337", Json.parse(updateValue).some)
        .returns(Future.successful(HttpResponse(true, 200, None)))

      val jsonRequest =
        s"""
           |{
           |"user_balance":12252,
           |"percentage":100
           |}
         """.stripMargin

      val fakeRequest = FakeRequest(PUT, s"/floats/institutions/SBM", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expected =
        s"""
           |{
           |"institution":"SBM",
           |"user_balance":12252,
           |"percentage":100}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }
  }

}
