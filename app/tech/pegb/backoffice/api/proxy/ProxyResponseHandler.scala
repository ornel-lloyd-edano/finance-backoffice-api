package tech.pegb.backoffice.api.proxy

import java.util.UUID

import cats.implicits._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{ControllerComponents, Request, ResponseHeader, Result}
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.api.{ApiController, ApiError, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, Logging, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@ImplementedBy(classOf[ProxyResponseHandlerImplementation])
trait ProxyResponseHandler {

  def handleApiProxyResponse[T, R](
    proxyRequest: ProxyRequest[R],
    additionHeaders: Seq[(String, String)] = Seq.empty,
    contentType: String = "application/json")(implicit request: Request[T], id: UUID): Future[Either[ApiError, Result]]
}

@Singleton
class ProxyResponseHandlerImplementation @Inject() (
    ws: WSClient,
    implicit val appConfig: AppConfig,
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents) extends ApiController(controllerComponents) with ProxyResponseHandler with RequiredHeaders with Logging {

  import ApiController._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  val hostUrl: String = appConfig.Hosts.MainBackofficeApi

  def handleApiProxyResponse[T, R](
    proxyRequest: ProxyRequest[R],
    additionHeaders: Seq[(String, String)] = Seq.empty,
    contentType: String = "application/json")(implicit request: Request[T], id: UUID): Future[Either[ApiError, Result]] = {

    logger.info(s"[handleApiProxyResponse] PROXY REQUEST ${proxyRequest}")

    val headers = proxyRequest.headers.toSeq

    val wsRequest = ws.url(s"$hostUrl/${proxyRequest.url}")
      .addHttpHeaders(headers: _*)
      .withRequestTimeout(appConfig.Hosts.TimeoutSeconds)
      .withMethod(proxyRequest.httpMethod)
      .addQueryStringParameters(proxyRequest.queryParameters: _*)

    //TODO remove this case match if front-end starts to send the content type application/json header in auth related request
    val finalRequest = proxyRequest.body.fold(wsRequest)(body ⇒ body.toString.trim match {
      case "" ⇒
        wsRequest.withBody(Json.parse("{}"))
      case value ⇒
        wsRequest.withBody(Json.parse(value))
    })

    logger.info(s"[handleApiProxyResponse] WSREQUEST $finalRequest")

    finalRequest
      .execute()
      .map {
        response ⇒
          Try {
            val responseHeaders = response.headers.map(h ⇒ (h._1, h._2.head))

            Result(ResponseHeader(response.status, responseHeaders), HttpEntity.Strict(response.bodyAsBytes, Some(response.contentType)))

          }.toEither.leftMap(_ ⇒ "invalid response".asUnknownApiError)
      }.recover {
        case error: Throwable ⇒
          logger.error(s"[handleApiProxyResponse] error encounter when forwarding request to internal endpoint $hostUrl/${proxyRequest.url}", error)
          Left(s"Unexpected internal error".asUnknownApiError)
      }

  }

}
