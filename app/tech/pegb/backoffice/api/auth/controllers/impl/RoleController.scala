package tech.pegb.backoffice.api.auth.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc._
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.auth._
import tech.pegb.backoffice.api.auth.dto.{RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.domain.auth.abstraction.RoleService
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.role.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.role.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoleController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    roleService: RoleService,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders with ConfigurationHeaders with controllers.RoleController {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._
  import RoleController._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def createRole(reactivate: Option[Boolean]): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      roleToCreateApi ← EitherT.fromEither[Future](ctx.body.toString.as(classOf[RoleToCreate], isDeserializationStrict)
        .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateRoleErrorMsg.some)))

      roleToCreate = roleToCreateApi.asDomain(doneAt, doneBy)

      createdRole ← EitherT(roleService.createActiveRole(roleToCreate, reactivateIfExisting = reactivate.getOrElse(false))).leftMap(_.asApiError())

    } yield createdRole.asApi.toJsonStr).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getRoleById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val criteria = (id.some, none, none).asDomain
    (for {
      optionalRole ← EitherT(roleService.getActiveRolesByCriteria(criteria.some, Seq.empty, none, none))
        .map(_.headOption).leftMap(_.asApiError())

      role ← EitherT.fromEither[Future](
        optionalRole.fold[Either[ApiError, String]](ApiErrors(ApiErrorCodes.NotFound, s"role $id not found").toLeft)(r ⇒ Right(r.asApi.toJsonStr)))

    } yield role).value.map(handleApiResponse(_))

  }

  def getRoles(orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val criteria = (none, none, none).asDomain

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrderBy(validOrderRoleFields)
        .map(_.mkString(",").asDomain)
        .leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(roleService.countActiveRolesByCriteria(criteria.some)).leftMap(_.asApiError())

      roles ← EitherT(roleService.getActiveRolesByCriteria(criteria.some, ordering, limit, offset)).leftMap(_.asApiError())

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)).leftMap(_.asApiError())
    } yield {

      (
        PaginatedResult(count, roles.map(_.asApi), limit, offset).toJsonStr,
        latestVersion)
    }).value.map(_.toTuple2FirstOneEither).map {

      case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
    }
  }

  def updateRole(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      roleToUpdateApi ← EitherT.fromEither[Future](ctx.body.toString.as(classOf[RoleToUpdate], isDeserializationStrict)
        .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateRoleErrorMsg.some)))

      roleToUpdate = roleToUpdateApi.asDomain(doneAt, doneBy)

      updatedRole ← EitherT(roleService.updateRole(id, roleToUpdate)).leftMap(_.asApiError())

    } yield updatedRole.asApi.toJsonStr).value.map(handleApiResponse(_))

  }

  def deleteRole(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      updatedAt ← EitherT.fromEither[Future](
        if (ctx.body.hasSomething) {
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError())
        } else {
          Right(GenericRequestWithUpdatedAt.empty)
        })

      result ← EitherT(roleService.removeRole(id, doneBy, doneAt.toLocalDateTimeUTC, updatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())

    } yield {
      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      id //result
    }).value.map(handleApiResponse(_)) //handleApiNoContentResponse

  }
}

object RoleController {

  val MalformedCreateRoleErrorMsg = "Malformed request to create a role. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateRoleErrorMsg = "Malformed request to update a role. Mandatory field is missing or value of a field is of wrong type."
  val validOrderRoleFields = Set("name", "level", "created_at", "updated_at")
}
