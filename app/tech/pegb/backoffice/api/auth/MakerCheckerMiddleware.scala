package tech.pegb.backoffice.api.auth

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.Request
import play.mvc.Http.HttpVerbs
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.api.makerchecker.dto.TaskToCreate
import tech.pegb.backoffice.api.makerchecker.json.Implicits._
import tech.pegb.backoffice.api.proxy.model.Module
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging}

import scala.util.Try

@Singleton
class MakerCheckerMiddleware @Inject() (conf: AppConfig) extends Logging {

  val exemptedEndpointSet: Set[String] = conf.MakerChecker.exemptedModules.split(",").map(_.replaceAll("\\s", "")).toSet
  val CreateTaskURI = "tasks"

  def createMakerCheckerRequest[R, T](proxyRequest: ProxyRequest[R])(implicit request: Request[T], module: Module, requestId: UUID): Try[ProxyRequest[_]] = Try {

    logger.info(s"[createRequest] creating MakerChecker POST /task request for ${request.method} ${proxyRequest.url}")

    val headers = request.headers.headers ++ proxyRequest.headers

    logger.info(s"[createRequest] original headers === $headers")

    logger.info(s"[createRequest] original body === ${request.body.toString}")

    val url = s"${conf.Hosts.MainBackofficeApiPlaceholder}/${proxyRequest.url}"
    val fullUrl = s"$url${(if (proxyRequest.queryParameters.nonEmpty) "?" else "") + proxyRequest.queryParameters.map(qp ⇒ s"${qp._1}=${qp._2}").mkString("&")}"
    val taskToCreate = TaskToCreate(
      url = fullUrl,
      body = getBodyForTask,
      headers = JsObject(headers.collect {
        case (name, value) if name.equalsIgnoreCase("request-id") ||
          name.equalsIgnoreCase("X-UserName") ||
          name.equalsIgnoreCase("pragma") ||
          name.equalsIgnoreCase("cache-control") ⇒ (name, JsString(value))
      }),
      verb = request.method,
      action = createAction(request.method, s"${proxyRequest.url}", module.name),
      module = module.name)

    val jsonRequest = Json.toJson(taskToCreate)

    logger.info(s"[createRequest] MakerChecker TaskToCreate $jsonRequest")

    proxyRequest.copy(
      httpMethod = HttpVerbs.POST,
      url = CreateTaskURI,
      body = Some(jsonRequest))
  }

  def isValidMakerCheckerRequest[T](implicit request: Request[T], module: Module, requestId: UUID): Boolean = {
    !(request.method == HttpVerbs.GET ||
      request.method == HttpVerbs.HEAD ||
      exemptedEndpointSet.contains(module.name))
  }

  private def getBodyForTask[T](implicit request: Request[T], requestId: UUID): Option[JsObject] = {

    if (request.method == HttpVerbs.PUT) Try(Json.parse(request.body.toString).as[JsObject]).fold(
      e ⇒ {
        logger.debug(s"failed to get body from request with id $requestId, reason: ${e.getMessage}")

        Some(Json.parse("""{"updated_at": null}""").as[JsObject])
      },
      body ⇒ Some(body))
    else Json.parse(request.body.toString).asOpt[JsObject]

  }

  private def createAction(verb: String, uri: String, module: String): String = {
    val uriArray = uri.split("/")
    val size = uriArray.size - 1

    if (verb == HttpVerbs.POST) {
      s"create"
    } else if (verb == HttpVerbs.DELETE) {
      s"delete"
    } else if (verb == HttpVerbs.PUT) {
      if (uriArray(size).isUuidOrInt) {
        "update"
      } else {
        s"${uriArray(size)}"
      }
    } else {
      s"HTTP[$verb]"
    }
  }
}

