package tech.pegb.backoffice.api.auth.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.auth.dto.{PermissionToCreate, PermissionToUpdate}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders, auth}
import tech.pegb.backoffice.domain.auth.abstraction.PermissionManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.permission.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.permission.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

class PermissionController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    permissionManagement: PermissionManagement,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with auth.controllers.PermissionController {

  import ApiController._
  import RequiredHeaders._
  import PermissionController._
  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def createPermission(reactivate: Option[Boolean]): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      permissionToCreateApi ← EitherT.fromEither[Future](
        ctx.body.as(classOf[PermissionToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreatePermissionErrorMsg.some)))

      permissionToCreate ← EitherT.fromEither[Future](
        permissionToCreateApi.asDomain(doneAt, doneBy).toEither
          .leftMap(_.asMalformedRequestApiError()))

      createdPermission ← EitherT(permissionManagement.createPermission(permissionToCreate, reactivate.getOrElse(false)))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())

    } yield createdPermission).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getPermissionById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    permissionManagement.getPermissionById(id).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getAllPermissions(
    businessUnitId: Option[UUID],
    roleId: Option[UUID],
    maybeUserId: Option[UUID],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      partialMatchSet ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(permissionPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(permissionValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = (None, businessUnitId, roleId, maybeUserId, None, partialMatchSet).asDomain()

      count ← EitherT(executeIfGET(permissionManagement.countByCriteria(criteria), NoCount.toFuture))
        .leftMap(_.asApiError("failed to get the count of Permissions".some))

      permissions ← EitherT(executeIfGET(
        permissionManagement.getPermissionByCriteria(criteria, ordering, limit, offset), NoResult.toFuture))
        .leftMap(_.asApiError("failed to get the Permissions".some))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria))
        .leftMap(_.asApiError("failed to get the latest version".some))

    } yield (PaginatedResult(count, permissions.map(_.asApi), limit, offset).toJsonStr, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
      }
  }

  def updatePermissionById(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      permissionToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.as(
        classOf[PermissionToUpdate],
        isDeserializationStrict).toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdatePermissionErrorMsg.some)))

      permissionToUpdate ← EitherT.fromEither[Future](
        permissionToUpdateApiDto.asDomain(doneAt, doneBy).toEither
          .leftMap(_.asMalformedRequestApiError()))

      updatedPermission ← EitherT(permissionManagement.updatePermissionById(id, permissionToUpdate))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())

    } yield updatedPermission).value.map(handleApiResponse(_))
  }

  def deletePermissionById(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      deleteWithUpdatedAt ← EitherT.fromEither[Future](
        if (ctx.body.hasSomething) {
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError())
        } else {
          Right(GenericRequestWithUpdatedAt.empty)
        })

      result ← EitherT(permissionManagement.deletePermissionById(
        id = id,
        updatedAt = doneAt.toLocalDateTimeUTC,
        updatedBy = doneBy,
        lastUpdatedAt = deleteWithUpdatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC))).leftMap(_.asApiError())

    } yield {
      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      id //result
    }).value.map(handleApiResponse(_)) //handleApiNoContentResponse

  }

}

object PermissionController {
  val MalformedCreatePermissionErrorMsg = "Malformed request to create a permission. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdatePermissionErrorMsg = "Malformed request to update a permission. Mandatory field is missing or value of a field is of wrong type."

  val permissionValidSorter = Set("id", "scope_id", "is_active",
    "created_at", "created_by", "updated_at", "updated_by")

  val permissionPartialMatchFields = Set("disabled", "id", "business_id", "role_id", "user_id", "scope_id")

}
