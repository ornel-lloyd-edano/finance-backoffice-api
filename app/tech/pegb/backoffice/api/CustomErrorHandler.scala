package tech.pegb.backoffice.api

import java.util.UUID

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import javax.inject.Singleton
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Logging

import scala.concurrent.Future

@Singleton
class CustomErrorHandler extends HttpErrorHandler with Logging {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    logger.error(s"Request header ${request} and statusCode $statusCode")
    val error = ApiError(UUID.randomUUID(), ApiErrorCodes.Unknown, message)
    logger.warn("A client error occurred: " + message)
    Future.successful(
      Status(statusCode)(error.json))
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val error = ApiError(UUID.randomUUID(), ApiErrorCodes.Unknown, exception.getMessage)
    logger.error("A server error occurred: ", exception)
    Future.successful(
      InternalServerError(error.json))
  }
}
