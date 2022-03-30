package tech.pegb.backoffice.api.auth.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.ApiController.{NoCount, NoResult}
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.auth.dto.{ScopeToCreate, ScopeToUpdate}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders, auth}
import tech.pegb.backoffice.domain.auth.abstraction.ScopeManagement
import tech.pegb.backoffice.domain.auth.dto.ScopeCriteria
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.scope.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.scope.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

class ScopeController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    scopeManagement: ScopeManagement,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with auth.controllers.ScopeController {

  import RequiredHeaders._
  import ScopeController._
  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  def createScope(reactivate: Option[Boolean]): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      scopeToCreateApi ← EitherT.fromEither[Future](
        ctx.body.as(classOf[ScopeToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateScopeErrorMsg.some)))

      scopeToCreate = scopeToCreateApi.asDomain(doneAt, doneBy)

      createdScope ← EitherT(scopeManagement.createScope(scopeToCreate, reactivate.getOrElse(false)))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())

    } yield createdScope).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getScopeById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    scopeManagement.getScopeById(id).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getAllScopes(orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val criteria = ScopeCriteria()

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(scopeValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(scopeManagement.countByCriteria(criteria), NoCount.toFuture))
        .leftMap(_.asApiError("failed to get the count of Scope".some))

      scopes ← EitherT(executeIfGET(
        scopeManagement.getScopeByCriteria(criteria, ordering, limit, offset), NoResult.toFuture))
        .leftMap(_.asApiError("failed to get Scopes".some))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria))
        .leftMap(_.asApiError("failed to get the latest version".some))

    } yield (PaginatedResult(count, scopes.map(_.asApi), limit, offset).toJsonStr, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
      }
  }

  def updateScopeById(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      scopeToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.as(
        classOf[ScopeToUpdate],
        isDeserializationStrict).toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateScopeErrorMsg.some)))

      scopeToUpdate = scopeToUpdateApiDto.asDomain(doneAt, doneBy)

      updatedScope ← EitherT(scopeManagement.updateScopeById(id, scopeToUpdate))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())

    } yield updatedScope).value.map(handleApiResponse(_))
  }

  def deleteScopeById(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
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

      result ← EitherT(scopeManagement.deleteScopeById(
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

object ScopeController {
  val MalformedCreateScopeErrorMsg = "Malformed request to create a scope. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateScopeErrorMsg = "Malformed request to update a scope. Mandatory field is missing or value of a field is of wrong type."

  val scopeValidSorter = Set("id", "parent_id", "name", "description", "is_active",
    "created_at", "created_by", "updated_at", "updated_by")
}
