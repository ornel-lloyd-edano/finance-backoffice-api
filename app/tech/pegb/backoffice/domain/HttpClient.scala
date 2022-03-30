package tech.pegb.backoffice.domain

import java.util.UUID

import cats.implicits._
import com.google.inject.{ImplementedBy, Inject}
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.{WSClient, WSRequest}
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging, WithExecutionContexts}

import scala.concurrent.Future

@ImplementedBy(classOf[HttpClientService])
trait HttpClient {

  def requestWithRedirect(baseUrl: String, queryStringParam: Seq[(String, String)]): Future[HttpResponse]

  def request(method: String, url: String, data: Option[JsValue]): Future[HttpResponse]

  def request(
    httpVerb: String,
    url: String,
    headers: Map[String, String],
    queryParams: Map[String, String],
    data: Option[JsValue],
    refId: UUID): Future[HttpResponse]

}

class HttpClientService @Inject() (
    config: AppConfig,
    executionContexts: WithExecutionContexts,
    wsClient: WSClient) extends HttpClient with Logging {

  import HttpClientService._

  private implicit val executionContext = executionContexts.genericOperations
  private val serviceCallTimeout = config.Hosts.TimeoutSeconds

  private def getSafeMethod(arg: String): Option[String] = {
    Seq("GET", "POST", "PUT", "DELETE", "PATCH").find(_ == arg.trim.toUpperCase)
  }

  def requestWithRedirect(baseUrl: String, queryStringParam: Seq[(String, String)]): Future[HttpResponse] = {
    wsClient.url(s"$baseUrl")
      .addQueryStringParameters(queryStringParam: _*)
      .withFollowRedirects(true)
      .get
      .map { response ⇒
        HttpResponse(success = response.status.isPositive, response.status, response.body.some)
      }
  }

  def request(method: String, url: String, data: Option[JsValue]): Future[HttpResponse] = {
    getSafeMethod(method).fold(
      HttpResponse(success = false, 500, s"Unexpected http method $method to for $url".toOption).toFuture) { safeMethod ⇒
        val baseRequest = wsClient.url(url)
          .withRequestTimeout(serviceCallTimeout)

        data.map(d ⇒ baseRequest.withBody(d)).getOrElse(baseRequest)
          .execute(safeMethod)
          .map { response ⇒
            val isSuccessful = response.status.isSuccessful
            val content = Option(response.body)
            logger.info(s"$safeMethod request to $url -d ${data.toString()} responded with status ${response.status}")
            if (!isSuccessful) {
              logger.warn(s"$safeMethod request to $url -d ${data.toString()} failed. Reason: ${response.body}")
            }
            HttpResponse(isSuccessful, response.status, content)
          }.recover {
            case error: Throwable ⇒
              logger.error(s"Error while calling service $safeMethod $url, -d $data", error)
              HttpResponse(success = false, 500, Option(error.getMessage))
          }

      }
  }

  def request(
    httpVerb: String,
    url: String,
    headers: Map[String, String],
    queryParams: Map[String, String],
    data: Option[JsValue],
    requestId: UUID): Future[HttpResponse] = {

    val baseRequest: WSRequest = wsClient.url(url)
      .addHttpHeaders(headers.toSeq: _*)
      .withRequestTimeout(serviceCallTimeout)
      .addQueryStringParameters(queryParams.toSeq: _*)

    getSafeMethod(httpVerb.trim.toUpperCase).fold(
      HttpResponse(success = false, 500, s"Unexpected http method $httpVerb to for $url".toOption).toFuture) { safeMethod ⇒
        data.fold(baseRequest)(js ⇒ baseRequest.withBody(js))
          .execute(safeMethod)
          .map { response ⇒
            val isSuccessful = response.status.isSuccessful
            val content = Option(response.body)
            logger.info(s"${httpVerb.toUpperCase} $url responded with status ${response.status}, request-id: $requestId")
            if (!isSuccessful) {
              logger.warn(s"${httpVerb.toUpperCase} request to $url failed, request-id: $requestId. Reason: ${response.body}")
            }
            HttpResponse(isSuccessful, response.status, content)
          }.recover {
            case error: Throwable ⇒
              logger.error(s"Error while calling ${httpVerb.toUpperCase} $url, request-id: $requestId", error)
              HttpResponse(success = false, 500, Option(error.getMessage))
          }
      }
  }
}

object HttpClientService {

  case class HttpResponse(success: Boolean, statusCode: Int, body: Option[String])

  implicit class RichStatus(val arg: Int) extends AnyVal {
    def isSuccessful: Boolean = arg >= 200 && arg <= 299
    def isOk: Boolean = arg == 200
    def isCreated: Boolean = arg == 201
    def isAccepted: Boolean = arg == 202
    def isNotFound: Boolean = arg == 404

    def isPositive: Boolean = arg < 400
  }

}
