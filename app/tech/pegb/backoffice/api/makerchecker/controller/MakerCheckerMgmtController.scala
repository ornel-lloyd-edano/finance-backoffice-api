package tech.pegb.backoffice.api.makerchecker.controller

import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.makerchecker.dto.ApproveTaskRequest
import tech.pegb.backoffice.api.makerchecker.json.Implicits._
import tech.pegb.backoffice.api.makerchecker.{Constants, MakerCheckerRequiredHeaders}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.domain.makerchecker.abstraction.MakerCheckerService
import tech.pegb.backoffice.domain.makerchecker.implementation.MakerCheckerServiceImpl
import tech.pegb.backoffice.domain.makerchecker.model.RoleLevels._
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.makerchecker.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.makerchecker.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class MakerCheckerMgmtController @Inject() (
    implicit
    val appConfig: AppConfig,
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    makerCheckerService: MakerCheckerService,
    latestVersionService: LatestVersionService) extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with MakerCheckerRequiredHeaders
  with makerchecker.MakerCheckerMgmtController {
  import ApiController._
  import MakerCheckerMgmtController._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout
  val PaginationMaxCap: Int = appConfig.PaginationMaxLimit

  override def getTask(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      validatedHeaders ← EitherT(validateRequiredHeaders)

      result ← EitherT(makerCheckerService.getTaskById(id).map(_.leftMap(_.asApiError())))
    } yield {

      result.asApiDetail(getRequestFrom, validatedHeaders.roleLevel, validatedHeaders.businessUnit).toJsonStrWithoutEscape
    }).value.map(handleApiResponse(_))
  }

  override def getTasksByCriteria(
    moduleName: Option[String],
    status: Option[String],
    createdAtDateFrom: Option[LocalDateTimeFrom],
    createdAtDateTo: Option[LocalDateTimeTo],
    isReadOnly: Option[Boolean],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      validatedHeaders ← EitherT(validateRequiredHeaders)

      _ ← EitherT.fromEither[Future]((createdAtDateFrom, createdAtDateTo, limit) match {
        case (Some(from), Some(to), _) if from.localDateTime.isAfter(to.localDateTime) ⇒

          ApiError(requestId, ApiErrorCodes.InvalidRequest, "date_from must be before or equal to date_to").toLeft
        case (_, _, Some(lim)) if lim > PaginationMaxCap ⇒

          ApiError(requestId, ApiErrorCodes.InvalidRequest, s"Limit provided(${limit.get}) is greater than max configured value of $PaginationMaxCap").toLeft

        case _ ⇒ Right(())
      })

      validatedPartialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(Constants.makerCheckerPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrderBy(Constants.makerCheckerSorter)
        .map(_.mkString(",").asDomain)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (moduleName, status, createdAtDateFrom, createdAtDateTo, isReadOnly, validatedPartialMatchFields).asDomain
          .toEither.leftMap(_.asInvalidRequestApiError("Invalid request to fetch tasks. A query parameter might be in the wrong format or not among the expected values.".toOption)))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(MakerCheckerServiceImpl.getValidDaoCriteria(criteria, validatedHeaders.roleLevel.asDomain, validatedHeaders.businessUnit))
        .map(_.leftMap(_ ⇒ "Failed to get latest version of tasks".asUnknownApiError)))

      total ← EitherT(executeIfGET(makerCheckerService.countTasksByCriteria(
        criteria,
        validatedHeaders.roleLevel.asDomain, validatedHeaders.businessUnit)
        .map(_.leftMap(_ ⇒ "Failed to get count tasks".asUnknownApiError))
        .futureWithTimeout
        .recover { case e: Throwable ⇒ Right(-1) }, NoCount.toFuture))

      results ← EitherT(executeIfGET(makerCheckerService.getTasksByCriteria(
        criteria,
        validatedHeaders.roleLevel.asDomain, validatedHeaders.businessUnit,
        ordering, limit, offset).map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {

      (PaginatedResult(total, results.map(_.asApi(
        getRequestFrom,
        validatedHeaders.roleLevel, validatedHeaders.businessUnit)), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, versionHeader) ⇒ handleApiResponse(result).withLatestVersionHeader(versionHeader)
    }

  }

  override def approveTask(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      validatedHeaders ← EitherT(validateRequiredHeaders)

      parsedRequest ← EitherT.fromEither[Future](if (ctx.body.trim.isEmpty) {
        Right(ApproveTaskRequest(None))
      } else {
        apiApproveTaskFormat.reads(Json.parse(ctx.body)).asEither
          .leftMap(_ ⇒ MalformedApproveRequest.asMalformedRequestApiError)
      })

      taskToApproveDomain ← EitherT.fromEither[Future](parsedRequest.asDomain(id, doneBy, doneAt,
        validatedHeaders.roleLevel, validatedHeaders.businessUnit)
        .toEither.leftMap(_.asInvalidRequestApiError(InvalidApproveRequest.toOption)))

      result ← EitherT(makerCheckerService.approvePendingTask(taskToApproveDomain)
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi(getRequestFrom, validatedHeaders.roleLevel, validatedHeaders.businessUnit).toJsonStrWithoutEscape

    }).value.map(handleApiResponse(_))
  }

  override def rejectTask(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      validatedHeaders ← EitherT(validateRequiredHeaders)

      parsedRequest ← EitherT.fromEither[Future](apiRejectTaskFormat.reads(Json.parse(ctx.body))
        .asEither.leftMap(_ ⇒ MalformedRejectRequest.asMalformedRequestApiError))

      dto ← EitherT.fromEither[Future](parsedRequest.asDomain(id, getRequestFrom, getRequestDate,
        validatedHeaders.roleLevel, validatedHeaders.businessUnit)
        .toEither.leftMap(_.asInvalidRequestApiError(InvalidRejectRequest.toOption)))

      result ← EitherT(makerCheckerService.rejectPendingTask(dto)
        .map(_.leftMap(_.asApiError())))
    } yield {

      result.asApi(getRequestFrom, validatedHeaders.roleLevel, validatedHeaders.businessUnit).toJsonStr

    }).value.map(handleApiResponse(_))

  }

  override def createTask: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      validatedHeaders ← EitherT(validateRequiredHeaders)

      parsedRequest ← EitherT.fromEither[Future](apiCreateTaskFormat.reads(ctx.body).asEither.leftMap(_ ⇒ MalformedCreateRequest.asMalformedRequestApiError))

      dto ← EitherT.fromEither[Future](parsedRequest.asDomain(doneBy, doneAt, validatedHeaders.roleLevel, validatedHeaders.businessUnit)
        .toEither.leftMap(_.log().asInvalidRequestApiError(InvalidCreateRequest.toOption)))

      result ← EitherT(makerCheckerService.createPendingTask(dto, requestId)
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApiCreated.toJsonStr

    }).value.map(handleApiResponse(_, SuccessfulStatuses.Accepted))
  }

  private def validateRequiredHeaders[T]()(implicit ctx: Request[T], requestId: UUID): Future[Either[ApiError, RequiredTaskHeaders]] = {
    (for {
      _ ← EitherT.fromEither[Future](getRequestApiKey match {
        case Some(apikey) if apikey == appConfig.ApiKeys.BackofficeAuth ⇒ Right("Source of request is trusted")
        case _ ⇒ Left(ApiKeyMissingOrMismatch.asNotAuthorizedApiError)
      })

      roleLevel ← EitherT.fromOption[Future](getRoleLevel, RoleLevelMissing.asInvalidRequestApiError)

      businessUnit ← EitherT.fromOption[Future](getBusinessUnitName, BusinessUnitMissing.asInvalidRequestApiError)
    } yield {
      RequiredTaskHeaders(roleLevel, businessUnit)
    }).value
  }
}

object MakerCheckerMgmtController {
  case class RequiredTaskHeaders(roleLevel: Int, businessUnit: String)

  val ApiKeyMissingOrMismatch = "Source of request is not trusted"
  val RoleLevelMissing = "Role level not found in request headers"
  val BusinessUnitMissing = "Business unit not found in request headers"

  val MalformedApproveRequest = "Malformed request to approve task. A mandatory field might be missing or its value is of wrong type."
  val MalformedRejectRequest = "Malformed request to reject task. A mandatory field might be missing or its value is of wrong type."
  val MalformedCreateRequest = "Malformed request to create task. A mandatory field might be missing or its value is of wrong type."

  val InvalidApproveRequest = "Invalid request to approve task. A field might be empty, in wrong format, or not in among the expected values."
  val InvalidRejectRequest = "Invalid request to reject task. A field might be empty, in wrong format, or not in among the expected values."
  val InvalidCreateRequest = "Invalid request to create task. A field might be empty, in wrong format, or not in among the expected values."
}
