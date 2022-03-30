package tech.pegb.backoffice.domain.makerchecker.implementation

import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.domain.makerchecker.abstraction.RequestCreator
import tech.pegb.backoffice.domain.makerchecker.model.MakerCheckerTask
import tech.pegb.backoffice.domain.{BaseService, HttpClient, ServiceError}
import tech.pegb.backoffice.util.{Logging, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class RequestCreatorImpl @Inject() (
    httpClient: HttpClient,
    executionContexts: WithExecutionContexts) extends RequestCreator with Logging with BaseService {

  private implicit val executionContext: ExecutionContext = executionContexts.genericOperations

  def createRequest(makerCheckerTask: MakerCheckerTask, hostPlaceHolder: String, actualHost: String): Future[ServiceResponse[Unit]] = {
    implicit val uuid: UUID = makerCheckerTask.id

    //without .toMap it will create mutable version
    val headers: Map[String, String] = makerCheckerTask.makerRequest.headers.value.map { case (k, v) ⇒ k → v.toString }.toMap

    (for {
      validUrl ← EitherT(Future.successful(makerCheckerTask.makerRequest.validateURL(actualHost, hostPlaceHolder)
        .leftMap(err ⇒ ServiceError.dtoMappingError(s"Malformed url. ${err.getMessage}", uuid.toOption))))

      validQueryParam ← EitherT(Future.successful(makerCheckerTask.makerRequest.getQueryParams.toEither
        .leftMap(_ ⇒ ServiceError.dtoMappingError(s"malformed query param, ${makerCheckerTask.makerRequest.getQueryParams}", uuid.toOption))))

      response ← EitherT {
        val verb = makerCheckerTask.makerRequest.verb
        val responseF = httpClient.request(
          httpVerb = verb.toString,
          url = validUrl.toString,
          headers = headers,
          queryParams = validQueryParam,
          data = makerCheckerTask.makerRequest.body,
          refId = uuid)

        responseF.map { response ⇒
          if (response.success) Right(())
          else {
            Left(unknownError(s"Received response status ${response.statusCode} ${response.body.map(b ⇒ s"with body $b").getOrElse("with no body")}"))
          }
        }.recover {
          case error: Throwable ⇒
            logger.error(s"Error while calling service url: $validUrl", error)
            Left(unknownError(s"Error while calling service url: $validUrl"))
        }
      }

    } yield response).value
  }
}
