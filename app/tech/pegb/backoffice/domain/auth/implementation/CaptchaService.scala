package tech.pegb.backoffice.domain.auth.implementation

import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.domain.{BaseService, HttpClient, ServiceError}
import tech.pegb.backoffice.util.{AppConfig, Logging, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class CaptchaService @Inject() (
    appConfig: AppConfig,
    executionContexts: WithExecutionContexts,
    httpClient: HttpClient) extends BaseService with Logging {

  implicit val ec = executionContexts.blockingIoOperations

  private val recaptchaSecret: String = appConfig.Authentication.recaptchaSecret

  private val baseRecaptchaUri = s"${appConfig.Authentication.recaptchaUrl}?secret=$recaptchaSecret"

  def checkCaptcha(captcha: String): Future[ServiceResponse[Unit]] = {

    httpClient.requestWithRedirect(baseRecaptchaUri, Seq(("response", captcha)))
      .map { response ⇒
        logger.info(s"[checkCaptcha] request to $baseRecaptchaUri?response=$captcha responded with status ${response.statusCode}")
        if (response.success) {
          Right(())
        } else {
          ServiceError.validationError(s"Invalid captcha: ${response.body}").asLeft[Unit]
        }
      }.recover {
        case error: Throwable ⇒
          logger.error(s"[checkCaptcha] Error while calling service $baseRecaptchaUri?response=$captcha", error)
          ServiceError.unknownError(error.getMessage).asLeft[Unit]
      }
  }

}
