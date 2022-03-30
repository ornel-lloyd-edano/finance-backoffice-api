package tech.pegb.backoffice.api

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.{Request, Result}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Utils}

trait RequiredHeaders {
  implicit val appConfig: AppConfig

  import ApiController._

  lazy val requestIdKey: String = appConfig.HeaderKeys.RequestIdKey
  lazy val requestDateKey: String = appConfig.HeaderKeys.RequestDateKey
  lazy val requestFromKey: String = appConfig.HeaderKeys.RequestFromKey
  lazy val apiKey: String = appConfig.HeaderKeys.ApiKey

  val defaultUnknownUser = "UNKNOWNUSER"

  def getRequestId[T](implicit ctx: Request[T]): UUID = {
    ctx.headers.get(requestIdKey)
      .map(uuid ⇒ UUID.fromString(uuid.stripEnclosingQuotes))
      .getOrElse(UUID.randomUUID())
  }

  def getRequestDate[T](implicit ctx: Request[T]): ZonedDateTime = {

    ctx.headers.get(requestDateKey)
      .map(d ⇒ ZonedDateTime.parse(d.stripEnclosingQuotes, DateTimeFormatter.ISO_ZONED_DATE_TIME))
      .getOrElse(Utils.now())
  }

  def getRequestFrom[T](implicit ctx: Request[T]): String = {
    ctx.headers.get(requestFromKey).map(_.stripEnclosingQuotes).getOrElse(defaultUnknownUser) //temporarily removed underscore from UNKNOWN_USER because of wallet core regex limitation. They will update later after demo
  }

  def getRequestApiKey[T](implicit ctx: Request[T]): Option[String] = {
    ctx.headers.get(apiKey)
  }

  def getTokenFromRequest[T](implicit ctx: Request[T], requestId: UUID): Either[ApiError, String] = {
    ctx.headers.get("authorization").fold[Either[ApiError, String]](Left("Authorization header is missing".asNotAuthorizedApiError)) {
      token ⇒
        val index = token.indexOf("Bearer ")

        if (index > -1) Right(token.substring(index + "Bearer ".length))
        else Left("Bearer token required".asInvalidRequestApiError)
    }
  }
}

object RequiredHeaders {

  implicit class RequiredResponseHeaders(val arg: Result) extends AnyVal {
    def withStandardHeaders(implicit appConfig: AppConfig): Result = {
      val accessControlExposeHeaders: String = appConfig.HeaderKeys.accessControlExposeHeaders
      arg.withHeaders(
        "Access-Control-Expose-Headers" -> accessControlExposeHeaders,
        HeaderNames.CONTENT_TYPE → ContentTypes.JSON).as(ContentTypes.JSON)
    }

    def withLatestVersionHeader(latestVersion: Option[String])(implicit appConfig: AppConfig): Result = {
      val latestVersionKey = appConfig.HeaderKeys.latestVersion
      arg.withHeaders(latestVersionKey → latestVersion.getOrElse(""))
    }
  }

}
