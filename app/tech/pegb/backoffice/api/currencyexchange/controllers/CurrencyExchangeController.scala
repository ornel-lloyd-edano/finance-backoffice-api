package tech.pegb.backoffice.api.currencyexchange.controllers

import java.util.UUID

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.currencyexchange.Constants
import tech.pegb.backoffice.api.currencyexchange.dto.{SpreadToCreate, SpreadToUpdate}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.currencyexchange.abstraction.{CurrencyExchangeManagement, SpreadsManagement}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.domain.api.currencyexchange.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.ExecutionContext

@Singleton
class CurrencyExchangeController @Inject() (
    implicit
    val appConfig: AppConfig,
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    fxService: CurrencyExchangeManagement,
    spreadsMgmt: SpreadsManagement)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with api.currencyexchange.CurrencyExchangeController {

  import ApiController._
  import RequiredHeaders._

  private implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  def getCurrencyExchangeByCriteria(
    id: Option[UUIDLike],
    currencyCode: Option[String],
    baseCurrency: Option[String],
    provider: Option[String],
    status: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT(orderBy.validateOrdering(Constants.currencyExchangeValidSorter)
        .map(_.map(o ⇒ o.copy(field = Constants.currencyExchangeColumnMapping.getOrElse(o.field, o.field))))
        .leftMap(_.log().asInvalidRequestApiError()).toFuture)

      partialMatchFields ← EitherT(partialMatch.validatePartialMatch(Constants.validCurrencyExchangesPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()).toFuture)

      criteria ← EitherT((id, currencyCode, baseCurrency, provider, status, partialMatchFields).asDomain
        .toEither.leftMap(_.log().asInvalidRequestApiError()).toFuture)

      countResult ← EitherT(executeIfGET(fxService.countCurrencyExchangeByCriteria(criteria)
        .map(_.leftMap(_ ⇒ "Failed counting currency exchange profiles".asUnknownApiError)), NoCount.toFuture))

      getResults ← EitherT(executeIfGET(fxService.getCurrencyExchangeByCriteria(criteria, ordering, limit, offset)
        .map(_.leftMap(_ ⇒ "Failed fetching currency exchange profiles".asUnknownApiError)), NoResult.toFuture))

      latestVersionResult ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of currency exchange profiles".asUnknownApiError)))

    } yield {
      (PaginatedResult(countResult, getResults.map(_.asApi), limit, offset).toJsonStr, latestVersionResult)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getCurrencyExchange(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    fxService.getCurrencyExchangeByUUID(id).map(result ⇒
      handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getCurrencyExchangeSpreads(
    currencyExchangeId: UUID,
    transactionType: Option[String],
    channel: Option[String],
    institution: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      ordering ← EitherT(orderBy.validateOrdering(Constants.validSpreadsOrderByFields)
        .leftMap(_.log().asInvalidRequestApiError()).toFuture)

      criteria ← EitherT((currencyExchangeId, transactionType, channel, institution).asDomain
        .toEither.leftMap(_.log().asInvalidRequestApiError()).toFuture)

      currencyExProfile ← EitherT(fxService.getCurrencyExchangeByUUID(currencyExchangeId)
        .map(_.leftMap(_.asApiError())))

      latestVersionResult ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of spreads".asUnknownApiError)))

      countResult ← EitherT(executeIfGET(spreadsMgmt.countSpreadByCriteria(criteria)
        .map(_.leftMap(_ ⇒ "Failed counting spreads".asUnknownApiError)), NoCount.toFuture))

      getResults ← EitherT(executeIfGET(spreadsMgmt.getSpreadByCriteria(criteria, ordering, limit, offset)
        .map(_.leftMap(_ ⇒ "Failed fetching spreads".asUnknownApiError)), NoResult.toFuture))

    } yield {
      (PaginatedResult(countResult, getResults.map(_.asApi), limit, offset).toJsonStr, latestVersionResult)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  override def activateFX(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    implicit val requestId: UUID = getRequestId

    (for {
      parsedRequest ← EitherT(ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log().asInvalidRequestApiError("Malformed request to activate currency exchange profile".toOption)).toFuture)

      result ← EitherT(fxService.activateFX(id = id, doneAt = doneAt.toLocalDateTimeUTC,
        doneBy = doneBy, lastUpdatedAt = parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  override def deactivateFX(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    implicit val requestId: UUID = getRequestId

    (for {
      parsedRequest ← EitherT(ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log().asInvalidRequestApiError("Malformed request to deactivate currency exchange profile".toOption)).toFuture)

      result ← EitherT(fxService.deactivateFX(id = id, doneAt = doneAt.toLocalDateTimeUTC,
        doneBy = doneBy, lastUpdatedAt = parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def createSpreads(currencyExchangeId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT(ctx.body.toString.as(classOf[SpreadToCreate], isDeserializationStrict)
        .toEither.leftMap(_.log().asInvalidRequestApiError(SpreadsController.MalformedCreateSpreadErrorMsg.toOption)).toFuture)

      spreadToCreate ← EitherT(parsedRequest.asDomain(currencyExchangeId, doneBy, doneAt)
        .toEither.leftMap(_.log().asInvalidRequestApiError(SpreadsController.InvalidCreateSpreadErrorMsg.toOption)).toFuture)

      result ← EitherT(spreadsMgmt.createSpread(spreadToCreate).map(_.leftMap(_.asApiError())))
    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))

  }

  override def updateCurrencyExchangeSpread(
    spreadId: UUID,
    fxId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      parsedRequest ← EitherT(ctx.body.toString.as(classOf[SpreadToUpdate], isDeserializationStrict)
        .toEither
        .leftMap(_.log().asInvalidRequestApiError(SpreadsController.MalformedUpdateSpreadErrorMsg.toOption)).toFuture)

      spreadToUpdate ← EitherT(parsedRequest.asDomain(spreadId, fxId, doneBy, doneAt)
        .toEither.leftMap(_.log().asInvalidRequestApiError(SpreadsController.InvalidUpdateSpreadErrorMsg.toOption)).toFuture)

      result ← EitherT(fxService.updateSpread(spreadToUpdate).map(_.leftMap(_.asApiError())))
    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  override def deleteCurrencyExchangeSpread(spreadId: UUID, fxId: UUID): Action[String] = LoggedAsyncAction(text) {
    implicit ctx ⇒
      implicit val requestId: UUID = getRequestId
      val doneAt = getRequestDate
      val doneBy = getRequestFrom

      (for {
        deleteRequest ← EitherT(ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
          .toEither.leftMap(_.log().asInvalidRequestApiError("Malformed request to delete spread".toOption)).toFuture)

        result ← EitherT(fxService.deleteSpread(spreadId, fxId, doneAt, doneBy,
          deleteRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC))
          .map(_.leftMap(_.asApiError())))

      } yield {
        result.asApi.toJsonStr
      }).value.map(handleApiResponse(_))
  }

  def batchActivateFX: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    fxService.batchActivateFX(doneAt.toLocalDateTimeUTC, doneBy).map(_.leftMap(_.asApiError()))
      .map(handleApiNoContentResponse(_))

  }

  def batchDeactivateFX: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    fxService.batchDeactivateFX(doneAt.toLocalDateTimeUTC, doneBy).map(_.leftMap(_.asApiError()))
      .map(handleApiNoContentResponse(_))
  }

}
