package tech.pegb.backoffice.api.transaction.controllers

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.transaction.dto.ManualTxnToCreate
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.transaction.abstraction.ManualTransactionManagement
import tech.pegb.backoffice.domain.transaction.dto.SettlementRecentAccountCriteria
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class ManualTransactionController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    manualTxnManagement: ManualTransactionManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with api.transaction.ManualTransactionController {

  import ApiController._
  import ManualTransactionController._
  import RequiredHeaders._
  import tech.pegb.backoffice.api.ApiErrors._

  implicit val executionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout = appConfig.FutureTimeout

  val PaginationMaxCap = appConfig.PaginationMaxLimit
  val DefaultOrdering = "-created_at,id"

  val DefaultRecentCount = appConfig.SettlementConstants.fxRecentCount

  def getManualTransactions(
    id: Option[UUIDLike],
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {

      _ ← EitherT.fromEither[Future](
        (dateFrom, dateTo, limit) match {
          case (Some(from), Some(to), _) if (from.localDateTime.isAfter(to.localDateTime)) ⇒
            "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft
          case (_, _, Some(limit)) if (limit > PaginationMaxCap) ⇒
            s"Limit provided(${limit}) is greater than PaginationMaxCap ${PaginationMaxCap}".asInvalidRequestApiError.toLeft
          case _ ⇒ (dateFrom, dateTo, limit).toRight
        })

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(validOrderByFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((id, dateFrom, dateTo).asDomain.toRight)

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError())))

      total ← EitherT(executeIfGET(manualTxnManagement.countManualTransactionsByCriteria(isGrouped = true, criteria)
        .map(_.leftMap(_.asApiError())), NoCount.toFuture))

      results ← EitherT(executeIfGET(manualTxnManagement.getManualTransactionsByCriteria(isGrouped = true, criteria, ordering, limit, offset)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))
    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  override def createManualTransaction: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(
        classOf[ManualTxnToCreate],
        isDeserializationStrict).toEither
        .leftMap(_.asMalformedRequestApiError(MalformedCreateRequestError.toOption)))

      dto ← EitherT.fromEither[Future](parsedRequest.asDomain(getRequestFrom, getRequestDate)
        .toEither.leftMap(_.asInvalidRequestApiError()))

      result ← EitherT(manualTxnManagement.createManualTransactions(dto)
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getSettlementFxHistory(
    provider: Option[String],
    fromCurrency: Option[String],
    toCurrency: Option[String],
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      _ ← EitherT.fromEither[Future](
        (dateFrom, dateTo) match {
          case (Some(from), Some(to)) if (from.localDateTime.isAfter(to.localDateTime)) ⇒
            "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft
          case _ ⇒ (dateFrom, dateTo).toRight
        })

      ordering ← EitherT.fromEither[Future](orderBy.orElse(Some("-created_at"))
        .validateOrdering(validOrderByFieldsForFxHistory)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = (provider, fromCurrency, toCurrency, dateFrom, dateTo).asDomain

      fxHistory ← EitherT(manualTxnManagement.getSettlementFxHistory(criteria, ordering, limit, offset))
        .leftMap(_.asApiError())

    } yield {
      fxHistory.map(_.asApi).toJsonStr
    }).value.map(handleApiResponse(_))

  }

  def getSettlementRecentAccount(limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val criteria = SettlementRecentAccountCriteria()

    val defaultLimit = limit.orElse(Some(DefaultRecentCount))

    (for {
      fxRecentAccounts ← EitherT(manualTxnManagement.getSettlementRecentAccount(criteria, defaultLimit, offset))
        .leftMap(_.asApiError())
    } yield {
      fxRecentAccounts.map(_.asApi).toJsonStr
    }).value.map(handleApiResponse(_))

  }

}

object ManualTransactionController {
  val validOrderByFields = Set("status", "created_at", "amount", "direction", "currency")
  val validOrderByFieldsForFxHistory = Set("created_at", "from_currency", "to_currency", "fx_provider", "fx_rate")
  val MalformedCreateRequestError = "Malformed request to create a manual settlement. Mandatory field is missing or value of field is wrong type."
  val InvalidCreateRequestError = "Invalid request to create a manual settlement. Value of a field is empty, not in the correct format or not among the expected values."
}
