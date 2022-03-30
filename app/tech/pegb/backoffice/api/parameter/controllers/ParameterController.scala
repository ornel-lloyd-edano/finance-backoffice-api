package tech.pegb.backoffice.api.parameter.controllers

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.parameter.dto.{ParameterToCreate, ParameterToUpdate}
import tech.pegb.backoffice.api.{ApiController, ApiError, ApiErrorCodes, ConfigurationHeaders, RequiredHeaders, parameter ⇒ api}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.parameter.abstraction.ParameterManagement
import tech.pegb.backoffice.mapping.api.domain.parameter.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.parameter.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

class ParameterController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    parameterMgmtService: ParameterManagement,
    implicit val appConfig: AppConfig)

  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with api.ParameterController {

  import ApiController._
  import ParameterController._
  import RequiredHeaders._
  import tech.pegb.backoffice.api.ApiErrors._

  implicit val ec: ExecutionContext = executionContexts.genericOperations

  def createParameter: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {

      dto ← EitherT.fromEither[Future](Json.parse(ctx.body).validate[ParameterToCreate].fold(
        _ ⇒ Left(ApiError(requestId, ApiErrorCodes.MalformedRequest, "Malformed json request to create parameter")),
        parsedParameterToCreate ⇒ Right(parsedParameterToCreate)))

      parameterToCreate ← EitherT.fromEither[Future](dto.copy(
        key = dto.key,
        explanation = dto.explanation,
        metadataId = dto.metadataId,
        platforms = dto.platforms)
        .asDomain(getRequestDate, getRequestFrom).toEither
        .leftMap(_.log().asMalformedRequestApiError(MalformedUpdateParameterErrorMsg.some)))

      createdParameter ← EitherT(parameterMgmtService.createParameter(parameterToCreate)
        .map(_.leftMap(_.asApiError("failed to create parameter".some))))

    } yield createdParameter.asApi.toJsonStrWithoutEscape).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getParameterById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      allParameters ← EitherT(parameterMgmtService.getParameters.map(_.leftMap(_.asApiError())))

      parametersByCriteria ← EitherT(
        parameterMgmtService.filterParametersByCriteria(allParameters, id.asDomain, Nil, None, None)
          .map(_.leftMap(_.asApiError(s"failed to get parameters by id $id".some))))

      parameter ← EitherT.fromEither[Future](parametersByCriteria.headOption
        .fold[Either[ApiError, String]](Left(ApiError(requestId, ApiErrorCodes.NotFound, s"Parameter with id $id not found")))(r ⇒ Right(r.asApi.toJsonStrWithoutEscape)))
    } yield parameter).value.map(handleApiResponse(_))

  }

  def getMetadataById(id: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      metadataId ← EitherT(parameterMgmtService.getMetadataSchemaById(id).map(_.map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())))
    } yield metadataId).value.map(handleApiResponse(_))
  }

  def getMetadata: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val result = parameterMgmtService.getMetadataSchema.map(_.map(_.map(_.asApi).toJsonStr).leftMap(_.asApiError()))

    result.map(handleApiResponse(_))
  }

  def getParametersByCriteria(
    key: Option[String],
    metadataId: Option[String],
    platforms: Option[String],
    maybeOrderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val criteria = (key, metadataId, platforms).asDomain

    (for {
      orderingSeq ← EitherT.fromEither[Future](
        maybeOrderBy.validateOrderBy(validOrderByFields)
          .map(_.mkString(",").asDomain)
          .leftMap(_.log().asInvalidRequestApiError()))

      baseParameters ← EitherT(parameterMgmtService.getParameters.map(_.leftMap(_.asApiError())))

      count ← EitherT(executeIfGET(parameterMgmtService.countParametersByCriteria(baseParameters, criteria)
        .map(_.leftMap(_.asApiError(s"failed to get parameters count".some))), NoCount.toFuture))

      parameters ← EitherT(
        executeIfGET(parameterMgmtService.filterParametersByCriteria(baseParameters, criteria, orderingSeq, limit, offset)
          .map(_.leftMap(_.asApiError(s"failed to get parameters by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(parameterMgmtService.getLatestVersion(baseParameters)
        .map(_.leftMap(_.asApiError(s"failed to get latest version for parameters".some))))

    } yield {

      (
        PaginatedResult(total = count, results = parameters.map(_.asApi), limit = limit, offset = offset).toJsonStrWithoutEscape,
        latestVersion)
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, versionHeader) ⇒ handleApiResponse(result).withLatestVersionHeader(versionHeader)

    }
  }

  def updateParameter(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      validateParameterToUpdate ← EitherT.fromEither[Future](
        Json.parse(ctx.body).validate[ParameterToUpdate].fold(
          _ ⇒ Left(ApiError(requestId, ApiErrorCodes.MalformedRequest, "Malformed json request to update parameter")),
          parsedParameterToUpdate ⇒ Right(parsedParameterToUpdate)))

      parameterToUpdate ← EitherT.fromEither[Future](validateParameterToUpdate.copy(
        value = validateParameterToUpdate.value,
        explanation = validateParameterToUpdate.explanation,
        metadataId = validateParameterToUpdate.metadataId,
        platforms = validateParameterToUpdate.platforms)
        .asDomain(getRequestDate, getRequestFrom).toEither
        .leftMap(_.log().asMalformedRequestApiError(MalformedUpdateParameterErrorMsg.some)))

      updatedResult ← EitherT(parameterMgmtService.updateParameter(id, parameterToUpdate)
        .map(_.leftMap(_.asApiError("failed to update parameter".some))))

    } yield updatedResult.asApi.toJsonStrWithoutEscape).value.map(handleApiResponse(_))
  }
}

object ParameterController {
  val validOrderByFields = Set("id", "key", "metadata_id", "created_at", "created_by", "updated_at", "updated_by")

  val MalformedCreateParameterErrorMsg = "Malformed request to create parameter. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateParameterErrorMsg = "Malformed request to update parameter. Mandatory field is missing or value of a field is of wrong type."

}
