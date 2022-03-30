package tech.pegb.backoffice.api.currencyexchange.controllers

import java.util.UUID

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.currencyexchange.dto.SpreadToCreateWithFxId
import tech.pegb.backoffice.api.currencyexchange.{Constants, SpreadsController ⇒ AbstractSpreadsController}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ApiErrors, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.currencyexchange.abstraction.SpreadsManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.currencyexchange.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class SpreadsController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    spreadsMgmt: SpreadsManagement,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents)
  with AbstractSpreadsController with RequiredHeaders with ConfigurationHeaders {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._
  import SpreadsController._

  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout
  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  //TODO add validation, establish if fxId exists and that spread id is related to it
  def getCurrencyExchangeSpread(fxId: UUID, id: UUID): Action[AnyContent] = getSpread(id)

  def getSpread(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    spreadsMgmt.getSpread(id).map(spread ⇒ handleApiResponse(spread.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getSpreadsByCriteria(id: Option[UUIDLike], currencyExchangeId: Option[UUIDLike],
    currency: Option[String], transactionType: Option[String],
    channel: Option[String], institution: Option[String],
    partialMatch: Option[String], orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(Constants.validSpreadsOrderByFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      partialMatchFields ← EitherT.fromEither[Future](
        partialMatch.validatePartialMatch(Constants.validSpreadsPartialMatchFields)
          .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((id, currencyExchangeId, currency, transactionType, channel, institution, partialMatchFields).asDomain
        .toEither.leftMap(_.log().asInvalidRequestApiError()))

      total ← EitherT(executeIfGET(spreadsMgmt.countSpreadByCriteria(criteria)
        .map(_.leftMap(_.asApiError(Some("Failed counting spreads")))), NoCount.toFuture))

      results ← EitherT(executeIfGET(spreadsMgmt.getSpreadByCriteria(criteria, ordering, limit, offset)
        .map(_.leftMap(_.asApiError(Some("Failed fetching spreads")))), NoResult.toFuture))

      latestVersionResult ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError(Some("Failed getting the latest version of spreads")))))

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersionResult)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  def createSpreads(): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      spreadToCreateApi ← EitherT.fromEither[Future](
        ctx.body.as(classOf[SpreadToCreateWithFxId], isDeserializationStrict).toEither
          .leftMap(_.log().asMalformedRequestApiError(MalformedCreateSpreadErrorMsg.toOption)))

      spreadToCreateDomain ← EitherT.fromEither[Future](spreadToCreateApi.asDomain(doneBy, doneAt).toEither
        .leftMap(_.log().asInvalidRequestApiError(InvalidCreateSpreadErrorMsg.toOption)))

      result ← EitherT(spreadsMgmt.createSpread(spreadToCreateDomain).map(_.map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())))

    } yield {
      result
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }
}

object SpreadsController {
  val MalformedCreateSpreadErrorMsg = "Malformed request to create a spread. Mandatory field is missing or value of a field is of wrong type."
  val InvalidCreateSpreadErrorMsg = "Invalid request to create spread. Value of a field is empty, not in the correct format or not among the expected values."
  val MalformedUpdateSpreadErrorMsg = "Malformed request to update a spread. Mandatory field is missing or value of a field is of wrong type."
  val InvalidUpdateSpreadErrorMsg = "Invalid request to update spread. Value of a field is empty, not in the correct format or not among the expected values."
}
