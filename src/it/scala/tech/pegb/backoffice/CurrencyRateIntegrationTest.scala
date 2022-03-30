package tech.pegb.backoffice

import cats.implicits._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import play.api.test.FakeRequest
import play.api.test.Helpers._

class CurrencyRateIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  private val baseCurrencyExchangePath = "/api/currency_rates"

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }
  
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "Get CurrencyRate API - Positive" should {
    "return list of currency rate" in {

      val resp = route(app, FakeRequest(GET, s"/api/currency_rates?show_empty=true").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"updated_at":"2019-02-25T00:00:00Z",
           |"results":[
           |{
           |"main_currency":{
           |"id":1,
           |"code":"AED",
           |"description":"Dirham"
           |},
           |"rates":[
           |{
           |"code":"USD",
           |"description":"US Dollar",
           |"buy_rate":{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"rate":0.010152
           |},
           |"sell_rate":{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"rate":0.010002
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"Euro",
           |"buy_rate":{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"rate":0.009062
           |},
           |"sell_rate":{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"rate":0.008920
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":2,
           |"code":"INR",
           |"description":"Indian Rupee"
           |},
           |"rates":[]
           |},
           |{
           |"main_currency":{
           |"id":3,
           |"code":"USD",
           |"description":"US Dollar"
           |},
           |"rates":[
           |{
           |"code":"AED",
           |"description":"Dirham",
           |"buy_rate":{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"rate":99.980000
           |},
           |"sell_rate":{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"rate":98.502758
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":4,
           |"code":"EUR",
           |"description":"Euro"
           |},
           |"rates":[
           |{
           |"code":"AED",
           |"description":"Dirham",
           |"buy_rate":{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"rate":112.102000
           |},
           |"sell_rate":{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"rate":110.350916
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":5,
           |"code":"CNY",
           |"description":"Chinese Yuan"
           |},
           |"rates":[]
           |},
           |{
           |"main_currency":{
           |"id":6,
           |"code":"CHF",
           |"description":"Swiss Franc"
           |},
           |"rates":[]
           |}
           |]}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe "2019-02-25T00:00".some

    }
    "return list of currency rate ordered by code" in {

      val resp = route(app, FakeRequest(GET, s"/api/currency_rates?order_by=code").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"updated_at":"2019-02-25T00:00:00Z",
           |"results":[
           |{
           |"main_currency":{
           |"id":1,
           |"code":"AED",
           |"description":"Dirham"
           |},
           |"rates":[
           |{
           |"code":"USD",
           |"description":"US Dollar",
           |"buy_rate":{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"rate":0.010152
           |},
           |"sell_rate":{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"rate":0.010002
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"Euro",
           |"buy_rate":{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"rate":0.009062
           |},
           |"sell_rate":{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"rate":0.008920
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":4,
           |"code":"EUR",
           |"description":"Euro"
           |},
           |"rates":[
           |{
           |"code":"AED",
           |"description":"Dirham",
           |"buy_rate":{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"rate":112.102000
           |},
           |"sell_rate":{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"rate":110.350916
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":3,
           |"code":"USD",
           |"description":"US Dollar"
           |},
           |"rates":[
           |{
           |"code":"AED",
           |"description":"Dirham",
           |"buy_rate":{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"rate":99.980000
           |},
           |"sell_rate":{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"rate":98.502758
           |}
           |}
           |]
           |}
           |]}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe "2019-02-25T00:00".some

    }
    "return list of currency rate ordered by code desc" in {

      val resp = route(app, FakeRequest(GET, s"/api/currency_rates?order_by=-code").withHeaders(AuthHeader)).get

      val expected =
        s"""{
           |"updated_at":"2019-02-25T00:00:00Z",
           |"results":[
           |{
           |"main_currency":{
           |"id":3,
           |"code":"USD",
           |"description":"US Dollar"
           |},
           |"rates":[
           |{
           |"code":"AED",
           |"description":"Dirham",
           |"buy_rate":{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"rate":99.980000
           |},
           |"sell_rate":{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"rate":98.502758
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":4,
           |"code":"EUR",
           |"description":"Euro"
           |},
           |"rates":[
           |{
           |"code":"AED",
           |"description":"Dirham",
           |"buy_rate":{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"rate":112.102000
           |},
           |"sell_rate":{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"rate":110.350916
           |}
           |}
           |]
           |},
           |{
           |"main_currency":{
           |"id":1,
           |"code":"AED",
           |"description":"Dirham"
           |},
           |"rates":[
           |{
           |"code":"USD",
           |"description":"US Dollar",
           |"buy_rate":{
           |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
           |"rate":0.010152
           |},
           |"sell_rate":{
           |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
           |"rate":0.010002
           |}
           |},
           |{
           |"code":"EUR",
           |"description":"Euro",
           |"buy_rate":{
           |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
           |"rate":0.009062
           |},
           |"sell_rate":{
           |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
           |"rate":0.008920
           |}
           |}
           |]
           |}
           |]}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
      headers(resp).get(versionHeaderKey) mustBe "2019-02-25T00:00".some

    }
    "return a currency_rate in GET /api/currency_rate/id" in {
      val resp = route(app, FakeRequest(GET, s"/api/currency_rates/1").withHeaders(AuthHeader)).get

      val expected =
        """
          |{
          |"main_currency":{
          |"id":1,
          |"code":"AED",
          |"description":"Dirham"
          |},
          |"rates":[
          |{
          |"code":"USD",
          |"description":"US Dollar",
          |"buy_rate":{
          |"id":"bb01ea86-de7b-414d-8f8a-757f101ccd13",
          |"rate":0.010152
          |},
          |"sell_rate":{
          |"id":"bd20bf3d-e3c2-49f3-b03a-5f66c140d9b4",
          |"rate":0.010002
          |}
          |},
          |{
          |"code":"EUR",
          |"description":"Euro",
          |"buy_rate":{
          |"id":"bbcd74ee-3a94-45b4-aac2-85856aaffd3f",
          |"rate":0.009062
          |},
          |"sell_rate":{
          |"id":"b566429b-e166-4cf6-83cb-7d8cf5ec9f09",
          |"rate":0.008920
          |}
          |}]
          |}""".stripMargin.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected

    }
    "respond 404 NotFound in /api/currency_rates/:id if currency was not found" in {

      val resp = route(app, FakeRequest(GET, s"/api/currency_rates/999").withHeaders(AuthHeader)).get

      status(resp) mustBe NOT_FOUND
      (contentAsJson(resp) \ "msg").get.toString should include("CurrencyId 999 not found")
    }
  }
}