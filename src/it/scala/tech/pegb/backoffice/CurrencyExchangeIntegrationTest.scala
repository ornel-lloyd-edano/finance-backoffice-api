package tech.pegb.backoffice

import java.util.UUID

import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import play.api.db.DBApi
import play.api.inject.{Binding, bind}
import play.api.libs.json.{JsString, JsValue}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import tech.pegb.backoffice.api.currencyexchange.dto.{CurrencyExchangeToRead, SpreadToRead}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.{HttpClient, HttpClientService}

import scala.concurrent.Future

@Ignore //unignore when mock wallet core is available in rancher
class CurrencyExchangeIntegrationTest extends PlayIntegrationTest with ScalaFutures {

  // from init-data/sql
  private val existingId: Long = 3L
  private val existingUuid: UUID = UUID.fromString("3a01ea86-de7b-414d-8f8a-757f101ccd13")

  override def additionalBindings: Seq[Binding[_]] = {
    val apiClient = new HttpClient() {
      def request(method: String, url: String, data: Option[JsValue]): Future[HttpResponse] = {
        val response = if (url.endsWith(s"/$existingId")) {
          data.get("status") match {
            case JsString(status) ⇒
              updateDb(existingId, status)
              HttpClientService.HttpResponse(
                success = true,
                statusCode = NO_CONTENT,
                None
              )
            case _ ⇒
              HttpClientService.HttpResponse(
                success = false,
                statusCode = BAD_REQUEST,
                None
              )
          }
        } else {
          HttpClientService.HttpResponse(
            success = false,
            statusCode = NOT_FOUND,
            None
          )
        }
        Future.successful(response)
      }

      def request(httpVerb: String, url: String, headers: Map[String, String], queryParams: Map[String, String], data: Option[JsValue], refId: UUID): Future[HttpResponse] = ???

      def requestWithRedirect(baseUrl: String, queryStringParam: Seq[(String, String)]): Future[HttpResponse] = ???
    }
    super.additionalBindings :+ bind[HttpClient].toInstance(apiClient)
  }

  "CurrencyExchange" should {
    "deactivate and activate currency exchange" in {
      val reqDeactivate = makePutJsonRequest(s"/api/currency_exchanges/$existingUuid/deactivate", "")
      val respDeactivate = route(app, reqDeactivate).get
      status(respDeactivate) mustBe OK
      val deactivatedOrError = contentAsString(respDeactivate).as(classOf[CurrencyExchangeToRead]).toEither
      deactivatedOrError.map(_.status) mustBe Right("inactive")
      val reqActivate = makePutJsonRequest(s"/api/currency_exchanges/$existingUuid/activate", "")
      val respActivate = route(app, reqActivate).get
      status(respActivate) mustBe OK
      val activatedOrError = contentAsString(respActivate).as(classOf[CurrencyExchangeToRead]).toEither
      activatedOrError.map(_.status) mustBe Right("active")
    }

    "update existing spread" in {
      val validSpreadId = UUID.fromString("938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0")
      val expectedSpread = BigDecimal("0.93")
      val payload =
        s"""{
           |  "spread": $expectedSpread
           |}
        """.stripMargin
      val reqUpdate = makePutJsonRequest(s"/api/currency_exchanges/$existingUuid/spreads/$validSpreadId", payload)
      val respUpdate = route(app, reqUpdate).get
      status(respUpdate) mustBe OK
      val updatedOrError = contentAsString(respUpdate).as(classOf[SpreadToRead]).toEither
      updatedOrError.map(_.spread) mustBe Right(expectedSpread)
    }

    "delete existing spread" in {
      val validSpreadId = UUID.fromString("938bc4b0-6d1e-4fd7-91bf-62e3205ae5b0")
      val payload = ""
      val reqDelete = makeDeleteJsonRequest(s"/api/currency_exchanges/$existingUuid/spreads/$validSpreadId", payload)
      val respDelete = route(app, reqDelete).get
      status(respDelete) mustBe OK
      val updatedOrError = contentAsString(respDelete).as(classOf[SpreadToRead]).toEither
      updatedOrError.map(_.id) mustBe Right(validSpreadId)
    }
  }

  private def updateDb(id: Long, status: String): Int = {
    val dbApi = inject[DBApi]
    dbApi.database("backoffice").withTransaction { implicit cxn ⇒
      anorm.SQL(s"UPDATE currency_rates SET status = {status} WHERE id = {id}")
        .on("status" → status, "id" → id)
        .executeUpdate()
    }
  }

}
