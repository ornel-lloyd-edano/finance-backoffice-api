package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.mvc.Request
import tech.pegb.backoffice.api.auth.model.ProxyRequest
import tech.pegb.backoffice.api.proxy.abstraction.ProxyRequestHandler
import tech.pegb.backoffice.api.proxy.model.Module
import tech.pegb.backoffice.domain.auth.model.BackOfficeUser
import tech.pegb.backoffice.util.{AppConfig, Logging}

import scala.util.Try

@Singleton
class ProxyRequestHandlerImpl @Inject() (conf: AppConfig) extends ProxyRequestHandler with Logging {

  val exemptedEndpointSet: Set[String] = conf.MakerChecker.exemptedModules.split(",").map(_.replaceAll("\\s", "")).toSet
  val apiKeyHeader: (String, String) = ("X-ApiKey", conf.ApiKeys.BackofficeAuth)

  def createRequest[T](
    user: Option[BackOfficeUser],
    internalUrl: String)(implicit request: Request[T], module: Module, requestId: UUID): Try[ProxyRequest[_]] = Try {

    val requiredHeaders = user.map(u ⇒ Seq(
      ("X-UserName", u.userName),
      ("X-RoleLevel", u.role.level.toString),
      ("X-BusinessUnit", u.businessUnit.name),
      apiKeyHeader)).getOrElse(Nil) ++
      request.headers.toSimpleMap.filter(head ⇒
        head._1.equalsIgnoreCase("request-date") ||
          head._1.equalsIgnoreCase("request-id"))

    logger.debug(s"[createRequest] original url = ${request.uri}")
    logger.debug(s"[createRequest] original queryParam = ${request.queryString}")

    val queryParams = request.queryString.toSeq.flatMap(d ⇒ d._2.map(v ⇒ (d._1, v)))
    logger.debug(s"[createRequest] recreated queryParam $queryParams")

    ProxyRequest(
      httpMethod = request.method,
      url = internalUrl,
      body = if (request.hasBody) Some(request.body) else None,
      queryParameters = queryParams,
      headers = requiredHeaders.toSet)
  }

}

