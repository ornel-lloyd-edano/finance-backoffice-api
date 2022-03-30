package tech.pegb.backoffice.api.currencyrate.controllers

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.currencyrate.Constants
import tech.pegb.backoffice.api.currencyrate.dto.CurrencyRateToRead.CurrencyRateResultToRead
import tech.pegb.backoffice.api.currencyrate.dto.MainCurrency
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders, _}
import tech.pegb.backoffice.domain.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.domain.currencyrate.abstraction.CurrencyRateManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.currencyrate.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.currencyrate.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CurrencyRateController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    currencyRateManagement: CurrencyRateManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with currencyrate.CurrencyRateController {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._
  import CurrencyRateController._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def getCurrencyRate(orderBy: Option[String], showEmpty: Option[Boolean]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(Constants.currencyRateValidSorter).leftMap(_.log().asInvalidRequestApiError()))

      currencyRateListF = executeIfGET(currencyRateManagement.getCurrencyRateList(ordering.headOption, showEmpty), NoResult.toFuture)
      latestVersionResultF = latestVersionService.getLatestVersion(CurrencyExchangeCriteria())

      currencyRateList ← EitherT(currencyRateListF.map(_.leftMap(_ ⇒ "Failed fetching currency rates".asUnknownApiError)))
      latestVersion ← EitherT(latestVersionResultF.map(_.leftMap(_ ⇒ "Failed getting the latest version of currency rates".asUnknownApiError)))

    } yield {
      (CurrencyRateResultToRead(
        updatedAt = latestVersion.map(d ⇒ LocalDateTime.parse(d).toZonedDateTimeUTC),
        results = currencyRateList.map(_.asApi)).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getCurrencyRateById(id: Long): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    currencyRateManagement.getCurrencyRateById(id)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))

  }

  def update(id: Int): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[MainCurrency], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError(Some(MalformedUpdateCurrencyRateErrorMsg))))

      results ← EitherT(currencyRateManagement.updateCurrencyRateList(
        id, parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC),
        parsedRequest.asDomain(doneAt, doneBy)).map(_.leftMap(_.asApiError())))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(CurrencyExchangeCriteria())
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of currency rates".asUnknownApiError)))

    } yield {
      CurrencyRateResultToRead(updatedAt = latestVersion.map(_.toZonedDateTimeUTC), results = results.map(_.asApi)).toJsonStr
    }).value.map(handleApiResponse(_))
  }
}

object CurrencyRateController {

  val MalformedUpdateCurrencyRateErrorMsg = "Malformed request to update a currency rate. Mandatory field is missing or value of a field is of wrong type."
}
