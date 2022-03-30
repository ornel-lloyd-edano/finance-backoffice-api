package tech.pegb.backoffice.api.reportsv2.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.model.{PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.reportsv2.Constants
import tech.pegb.backoffice.api.reportsv2.controllers
import tech.pegb.backoffice.api.reportsv2.dto.{ReportDefinitionToCreate, ReportDefinitionToRead, ReportDefinitionToUpdate}
import tech.pegb.backoffice.api.{ApiController, ApiErrors, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.report.abstraction.ReportManagement
import tech.pegb.backoffice.domain.report.dto.ReportDefinitionCriteria
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.report.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.report.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ReportDefinitionController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    reportDefinitionManagement: ReportManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with controllers.ReportDefinitionController {

  import ApiController._
  import ApiErrors._
  import PaginatedResult.paginatedResultWrites
  import ReportDefinitionToCreate.f
  import ReportDefinitionToRead.f
  import ReportDefinitionToUpdate.f
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def createReportDefinition: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      parsedRequest ← EitherT.fromEither[Future](Try(ctx.body.as[ReportDefinitionToCreate]).toEither
        .leftMap(_.log().asMalformedRequestApiError()))

      result ← EitherT(reportDefinitionManagement.createReportDefinition(parsedRequest.asDomain(doneBy, doneAt)))
        .leftMap(_.asApiError())

    } yield {
      result.asApi
    }).value.map(handleApiResponse(_, defaultOkStatus = SuccessfulStatuses.Created))
  }

  def getReportDefinition(name: Option[String], partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      partialMatchSet ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(Constants.reportDefinitionPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(Constants.reportDefinitionValidSorter)
        .map(_.map(ord ⇒ Constants.apiToDbNames.get(ord.field).map(dbName ⇒ ord.copy(field = dbName)).getOrElse(ord)))
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = ReportDefinitionCriteria(name = name, partialMatchFields = partialMatchSet)

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria))
        .leftMap(_.asApiError("failed to get the latest version".some))

      count ← EitherT(executeIfGET(reportDefinitionManagement.countReportDefinitionByCriteria(criteria).futureWithTimeout, NoCount.toFuture))
        .leftMap(_.asApiError("failed to get the count of I18N string".some))

      reportDefinition ← EitherT(executeIfGET(
        reportDefinitionManagement.getReportDefinitionByCriteria(criteria, ordering, limit, offset).futureWithTimeout, NoResult.toFuture))
        .leftMap(_.asApiError("failed to get the list of Report Definition".some))

    } yield (PaginatedResult(count, reportDefinition.map(_.asApi), limit, offset), latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
      }

  }

  def getReportDefinitionById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    reportDefinitionManagement.getReportDefinitionById(id).map(serviceResponse ⇒
      handleApiResponse(serviceResponse
        .map(_.asApi)
        .leftMap(_.asApiError())))
  }

  def updateReportDefinition(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      parsedRequest ← EitherT.fromEither[Future](Try(ctx.body.as[ReportDefinitionToUpdate]).toEither
        .leftMap(_.log().asMalformedRequestApiError()))

      result ← EitherT(reportDefinitionManagement.updateReportDefinition(id, parsedRequest.asDomain(doneBy, doneAt)))
        .leftMap(_.asApiError())

    } yield {
      result.asApi
    }).value.map(handleApiResponse(_))
  }

  //TODO add GenericUpdatedAtRequest
  def deleteReportDefinitionById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    reportDefinitionManagement.deleteReportDefinitionById(id).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.leftMap(_.asApiError())))
  }

}
