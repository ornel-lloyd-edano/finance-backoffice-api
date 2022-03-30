package tech.pegb.backoffice.api.auth.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.api.auth.controllers.{BackOfficeUserController ⇒ BackOfficeUserControllerTrait}
import tech.pegb.backoffice.api.auth.dto.{BackOfficeUserToCreate, BackOfficeUserToUpdate}
import tech.pegb.backoffice.domain.auth.abstraction.BackOfficeUserService
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.backofficeuser.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.Implicits._

@Singleton
class BackOfficeUserController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig,
    backOfficeUserService: BackOfficeUserService) extends ApiController(controllerComponents)
  with RequiredHeaders with ConfigurationHeaders with BackOfficeUserControllerTrait {

  import BackOfficeUserController._
  import tech.pegb.backoffice.api.ApiErrors._
  import ApiController._
  import RequiredHeaders._

  implicit val ec = executionContexts.genericOperations

  def createBackOfficeUser(reactivate: Option[Boolean]): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      dto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BackOfficeUserToCreate], isDeserializationStrict)
          .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to create back_office_user. A mandatory field might be missing or its value is of wrong type.".toOption)))

      result ← EitherT(backOfficeUserService.createBackOfficeUser(
        dto.asDomain(getRequestFrom, getRequestDate),
        reactivate.getOrElse(false))).leftMap(_.asApiError())

    } yield {
      result.asApi.toJsonStr

    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getBackOfficeUserById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      criteria ← EitherT.fromEither[Future]((Option(id.toString), None, None, None, None, None, None, None, Set.empty[String]).asDomain
        .toEither.leftMap(_.log().asInvalidRequestApiError("Invalid request to fetch back_office_user by id. A query param may not be in the correct format or not among the expected values.".toOption)))

      result ← EitherT(backOfficeUserService.getActiveBackOfficeUsersByCriteria(criteria.toOption, Seq.empty, None, None)
        .map(_.fold(_.asApiError().toLeft, _.headOption match {
          case Some(found) ⇒ found.asApi.toJsonStr.toRight
          case None ⇒ s"Backoffice user with id [$id] was not found.".asNotFoundApiError.toLeft
        })))
    } yield {
      result
    }).value.map(handleApiResponse(_))
  }

  def getBackOfficeUsers(userName: Option[String], firstName: Option[String], lastName: Option[String],
    email: Option[String], phoneNumber: Option[String], roleId: Option[String],
    businessUnitId: Option[String], scopeId: Option[String], partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(validOrderBy).leftMap(_.log().asInvalidRequestApiError()))

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(validPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← {
        EitherT.fromEither[Future]((None, roleId, businessUnitId, userName, firstName, lastName, email, phoneNumber, partialMatchFields).asDomain
          .toEither.leftMap(_.log().asInvalidRequestApiError("Invalid request to fetch back_office_users. A query param may not be in the correct format or not among the expected values.".toOption)))
      }

      latestVersion ← {
        EitherT(latestVersionService.getLatestVersion(criteria)
          .map(_.leftMap(_ ⇒ "Failed getting the latest version of back_office_users".asUnknownApiError)))
      }

      total ← EitherT(executeIfGET(
        backOfficeUserService.countActiveBackOfficeUsersByCriteria(criteria.toOption),
        NoCount.toFuture)).leftMap(_.asApiError())

      results ← EitherT(executeIfGET(
        backOfficeUserService.getActiveBackOfficeUsersByCriteria(criteria.toOption, ordering, limit, offset),
        NoResult.toFuture)).leftMap(_.asApiError())

    } yield {
      (PaginatedResult(total, results = results.map(_.asApi), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def updateBackOfficeUser(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      dto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BackOfficeUserToUpdate], isDeserializationStrict)
          .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to update back_office_user. A mandatory field might be missing or its value is of wrong type.".toOption)))

      domainDto ← EitherT.fromEither[Future](dto.asDomain(getRequestFrom, getRequestDate)
        .toEither.leftMap(_.log().asInvalidRequestApiError("Invalid request to back_office_user. A field might be in the wrong format or not among the expected values.".toOption)))

      result ← EitherT(backOfficeUserService.updateBackOfficeUser(id, domainDto))
        .leftMap(_.asApiError())

    } yield {
      result.asApi.toJsonStr

    }).value.map(handleApiResponse(_))
  }

  def deleteBackOfficeUser(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      dto ← EitherT.fromEither[Future](
        if (ctx.body.hasSomething) {
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to remove back_office_user. A mandatory field might be missing or its value is of wrong type.".toOption))
        } else {
          Right(GenericRequestWithUpdatedAt.empty)
        })

      result ← EitherT(backOfficeUserService.removeBackOfficeUser(id, (getRequestFrom, getRequestDate, dto.lastUpdatedAt).asDomain))
        .leftMap(_.asApiError())

    } yield {
      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      id //result
    }).value.map(handleApiResponse(_)) //handleApiNoContentResponse
  }
}

object BackOfficeUserController {
  val validOrderBy = Set("id", "user_name", "last_name", "email", "phone_number", "created_at", "updated_at", "role_id", "business_unit_id", "scope_id")
  val validPartialMatchFields = Set("disabled", "user_name", "first_name", "last_name", "email", "phone_number", "role_id", "business_unit_id", "scope_id")

}
