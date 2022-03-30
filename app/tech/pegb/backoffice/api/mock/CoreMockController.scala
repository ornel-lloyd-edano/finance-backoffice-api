package tech.pegb.backoffice.api.mock

import java.util.UUID

import cats.syntax.either._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.mock.model.MockCurrencyRateToUpdate
import tech.pegb.backoffice.api.{ApiController, ApiErrors, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.currencyexchange.abstraction.CurrencyExchangeManagement
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.ExecutionContext

@Singleton
class CoreMockController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    currencyExchangeManagement: CurrencyExchangeManagement,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders {

  import ApiErrors._

  private implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  def notifyCore(): Action[AnyContent] = LoggedAction { request ⇒
    val json = request.body.asJson.get

    logger.info(s"[notifyCore] received json request:\n${json}.\n Returning Ok Response..")
    Ok
  }

  def updateCurrencyRateStatus(currencyExchangeId: Int): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId = UUID.randomUUID()
    logger.info(s"[updateCurrencyRateStatus] received request:\n${ctx.body}")

    ctx.body.as(classOf[MockCurrencyRateToUpdate], isDeserializationStrict).fold(
      error ⇒ error.asApiError().toLeft.toFuture,
      apiDto ⇒ currencyExchangeManagement
        .updateCurrencyExchangeStatus(currencyExchangeId, apiDto.status).map(result ⇒
          result.map(_.toString)
            .leftMap(_.asApiError()))).map(handleApiResponse(_))
  }
}
