package tech.pegb.backoffice.api.aggregations.controllers.impl

import java.util.UUID

import cats.implicits._
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import play.mvc.Http.HttpVerbs
import tech.pegb.backoffice.api.aggregations.dto._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait AmountAggregationUtil extends Logging with Results {

  val httpClient: HttpClient
  val appConfig: AppConfig

  val aggregationURL: String = appConfig.AggregationEndpoints.aggregation

  def getAmountAggregationSeq(
    aggregation: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom] = None,
    dateTo: Option[LocalDateTimeTo] = None,
    transactionType: Option[String] = None,
    accountType: Option[String] = None,
    institution: Option[String] = None,
    frequency: Option[String] = None,
    groupBy: Option[String] = None,
    userType: Option[String] = None,
    notLikeThisAccountNumber: Option[String] = None,
    step: Option[Int] = None)(implicit requestId: UUID, executionContext: ExecutionContext): Future[Either[Result, Seq[AmountAggregation]]] = {

    val sb = new StringBuilder(aggregationURL)

    sb.append(s"?aggregation=${aggregation.sanitize}")
    sb.append(s"&currency_code=${currencyCode.sanitize}")
    dateFrom.foreach(from ⇒ sb.append(s"&date_from=${from.localDateTime}"))
    dateTo.foreach(to ⇒ sb.append(s"&date_to=${to.localDateTime}"))
    transactionType.foreach(x ⇒ sb.append(s"&transaction_type=${x.sanitize}"))
    accountType.foreach(x ⇒ sb.append(s"&account_type=${x.sanitize}"))
    institution.foreach(x ⇒ sb.append(s"&institution=${x.sanitize}"))
    frequency.foreach(x ⇒ sb.append(s"&frequency=${x.sanitize}"))
    groupBy.foreach(gby ⇒ sb.append(s"&group_by=${gby.sanitize}"))
    userType.foreach(x ⇒ sb.append(s"&user_type=${x.sanitize}"))
    notLikeThisAccountNumber.foreach(accNum ⇒ sb.append(s"&not_like_this_account_number=${accNum.sanitize}"))
    step.foreach(s ⇒ sb.append(s"&step=$s"))

    sendHttpCall[Seq[AmountAggregation]](sb.toString(), "results".some)
  }

  def sendHttpCall[T](url: String, specificField: Option[String] = None)(implicit requestId: UUID, reads: Reads[T], executionContext: ExecutionContext): Future[Either[Result, T]] = {
    logger.info(s"Sending GET $url")

    httpClient.request(HttpVerbs.GET, url, None) map { resp ⇒

      if (resp.success) {
        for {
          jsonBody ← Either.cond(resp.body.isDefined, resp.body.get, BadRequest(s"$url responded empty body"))
          validJson ← Try(Json.parse(jsonBody)).toEither.leftMap(_ ⇒ BadRequest(s"$jsonBody is not a valid json"))
          jsonToConvert ← specificField.fold(validJson.asRight[Result])(field ⇒ (validJson \ field).toEither.leftMap(_ ⇒ BadRequest(s"$jsonBody is not a valid json")))
          responseT ← jsonToConvert.validate[T].asEither
            .leftMap(error ⇒ {
              logger.error(s"error while parsing response $jsonBody -> $error")
              BadRequest(malformedPaginatedAmountAggregationResponse(url))
            })
        } yield {
          responseT
        }
      } else {
        Status(resp.statusCode)(resp.body.getOrElse(s"Missing Error body from $url")).asLeft[T]
      }
    }

  }

  def malformedPaginatedAmountAggregationResponse(url: String) = s"Malformed response received from url $url"

}
