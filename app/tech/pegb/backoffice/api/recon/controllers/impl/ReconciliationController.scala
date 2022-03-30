package tech.pegb.backoffice.api.recon.controllers.impl

import java.time.LocalDate
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api.recon.controllers
import tech.pegb.backoffice.api.recon.dto.InternReconDailySummaryResultResolve
import tech.pegb.backoffice.api.swagger.model.{InternReconPaginatedResult, InternReconSummaryPaginatedResult}
import tech.pegb.backoffice.api.{ApiController, ApiErrors, RequiredHeaders}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.reconciliation.abstraction.Reconciliation
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.reconciliation.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.recon.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Api(value = "Reconciliation", produces = "application/json", consumes = "application/json")
@Singleton
class ReconciliationController @Inject() (
    implicit
    val appConfig: AppConfig,
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    reconciliation: Reconciliation,
    latestVersionService: LatestVersionService) extends ApiController(controllerComponents)
  with RequiredHeaders with controllers.ReconciliationController {

  import ApiController._
  import ApiErrors._
  import ReconciliationController._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  val defaultLimit: Int = appConfig.PaginationLimit
  val defaultOffset: Int = appConfig.PaginationOffset

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.recon.dto.InternReconSummaryToRead], message = "")))
  def getReconciliationSummaryById(id: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val optionalCriteria = (id.some, none, none, none, none, none, none, none, Set.empty[String]).asDomain.some

    reconciliation.getInternalReconSummaries(
      optionalCriteria,
      maybeOrderedBy = Seq.empty, maybeLimit = None, maybeOffset = None)
      .map(result ⇒ handleApiResponse(result.map(_.map(_.asApi).toJsonStr).leftMap(_.asApiError())))
  }

  @ApiOperation(value = "Returns list of internal reconciliation")
  @ApiResponses(Array(new ApiResponse(code = 200, response = classOf[InternReconSummaryPaginatedResult], message = "")))
  def getInternalRecon(
    maybeId: Option[String],
    maybeUserId: Option[String],
    maybeAnyCustomerName: Option[String],
    maybeAccountNumber: Option[String],
    maybeAccountType: Option[String],
    maybeStatus: Option[String],
    maybeStartReconDate: Option[LocalDateTimeFrom],
    maybeEndReconDate: Option[LocalDateTimeTo],
    maybeOrderBy: Option[String],
    partialMatch: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val limit = maybeLimit.orElse(Some(defaultLimit))
    val offset = maybeOffset.orElse(Some(defaultOffset))

    (for {
      _ ← EitherT.fromEither[Future](
        (maybeStartReconDate, maybeEndReconDate) match {
          case (Some(from), Some(to)) if from.localDateTime.isAfter(to.localDateTime) ⇒
            "start_date must be before or equal to end_date".asInvalidRequestApiError.toLeft
          case _ ⇒
            (maybeStartReconDate, maybeEndReconDate).toRight
        })

      orderingSeq ← EitherT.fromEither[Future](
        maybeOrderBy.validateOrderBy(ValidReconSorter)
          .map(_.mkString(",").asDomain)
          .leftMap(_.log().asInvalidRequestApiError()))

      partialMatch ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(ValidSummaryPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = (maybeId, maybeAccountNumber, maybeAccountType, maybeUserId, maybeAnyCustomerName, maybeStatus, maybeStartReconDate
        .map(_.localDateTime), maybeEndReconDate.map(_.localDateTime), partialMatch).asDomain.some

      count ← EitherT(executeIfGET(reconciliation.countInternalReconSummaries(criteria)
        .map(_.leftMap(_.asApiError("failed getting the count of recon summary".some))), NoCount.toFuture))

      ordering = orderingSeq.map(o ⇒ if (o.field == createdAtField) o.copy(field = reconDateField) else o)

      result ← EitherT(
        executeIfGET(reconciliation.getInternalReconSummaries(criteria, maybeOrderedBy = ordering,
          maybeLimit = limit, maybeOffset = offset)
          .map(_.leftMap(_.asApiError("failed to get recon summary by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed getting the latest version of recon summary".some))))
    } yield {

      (PaginatedResult(total = count, results = result.map(_.asApi), limit = limit, offset = offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {

      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  @ApiOperation(value = "Returns list of internal reconciliation incidents")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[InternReconPaginatedResult], message = "")))
  def getInternalReconIncidents(
    maybeReconId: Option[String],
    maybeAccountNumber: Option[String],
    maybeStartReconDate: Option[LocalDateTimeFrom],
    maybeEndReconDate: Option[LocalDateTimeTo],
    maybeOrderBy: Option[String],
    partialMatch: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val limit = maybeLimit.orElse(Some(defaultLimit))
    val offset = maybeOffset.orElse(Some(defaultOffset))

    (for {
      _ ← EitherT.fromEither[Future](
        (maybeStartReconDate, maybeEndReconDate) match {
          case (Some(from), Some(to)) if from.localDateTime.isAfter(to.localDateTime) ⇒
            "start_recon_date must be before or equal to end_recon_date".asInvalidRequestApiError.toLeft
          case _ ⇒
            (maybeStartReconDate, maybeEndReconDate).toRight
        })

      orderingSeq ← EitherT.fromEither[Future](maybeOrderBy.validateOrderBy(ValidReconIncidentsSorter)
        .map(_.mkString(",").asDomain).leftMap(_.log().asInvalidRequestApiError()))

      partialMatch ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(ValidInternReconPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = (maybeReconId, maybeAccountNumber, None, //NOT exposed to front
        maybeStartReconDate.map(_.localDateTime), maybeEndReconDate.map(_.localDateTime), partialMatch).asDomain.some

      count ← EitherT(executeIfGET(reconciliation.countInternalReconResults(criteria)
        .map(_.leftMap(_.asApiError("failed getting the count of recon incidents".some))), NoCount.toFuture))

      ordering = orderingSeq.map(o ⇒ if (o.field == createdAtField) o.copy(field = reconDateField) else o)

      result ← EitherT(executeIfGET(reconciliation.getInternalReconResults(criteria, maybeOrderedBy = ordering,
        maybeLimit = limit, maybeOffset = offset)
        .map(_.leftMap(_.asApiError("failed getting recon incidents by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed getting the latest version of recon incidents".some))))
    } yield {

      (PaginatedResult(total = count, results = result.map(_.asApi), limit = limit, offset = offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {

      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  def externalRecon(
    thirdParty: String,
    source: Option[String],
    startDate: LocalDate,
    endDate: LocalDate): Action[AnyContent] = ???

  def getTxnsForThirdPartyRecon(thirdParty: String, startDate: LocalDate, endDate: LocalDate): Action[AnyContent] = ???

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.recon.dto.InternReconSummaryToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.InternReconDailySummaryResultResolve",
      example = "", //not showing correctly
      paramType = "body",
      name = "InternReconDailySummaryResultResolve")))
  def updateReconStatus(id: String): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {

      updateApiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[InternReconDailySummaryResultResolve], isStrict = false)
        .toEither.leftMap(_.log().asMalformedRequestApiError(MalformedUpdateReconSummaryErrorMsg.some)))

      updated ← EitherT(reconciliation.resolveInternalReconSummary(id, updateApiDto.asDomain(getRequestFrom, getRequestDate.toLocalDateTimeUTC))
        .map(_.map(_.asApi.toJsonStr)
          .leftMap(_.asApiError())))

    } yield updated).value.map(handleApiResponse(_))
  }

}

object ReconciliationController {

  //TODO use recon_date instead of created_at once front end fix issue
  val createdAtField = "created_at"
  val reconDateField = "recon_date"

  val ValidReconIncidentsSorter = Set("incident_id", "recon_id", createdAtField, reconDateField, "account_number",
    "currency", "txn_id", "txn_sequence", "txn_direction", "txn_date",
    "txn_amount", "balance_before", "balance_after")

  val DefaultIncidentsOrdering = s"-$createdAtField"

  val ValidReconSorter = Set("id", createdAtField, reconDateField, "account_number", "acc_type", "user", "user_id",
    "status", "incidents", "difference", "total_value", "total_txn", "txn_count")

  val ValidSummaryPartialMatchFields = Set("user_id", "account_number", "any_customer_name")

  val ValidInternReconPartialMatchFields = Set("recon_id", "account_number")

  val DefaultReconOrdering = s"-$createdAtField"

  val MalformedUpdateReconSummaryErrorMsg = "Malformed request to update reconciliation summary. Mandatory field is missing or value of a field is of wrong type."
}
