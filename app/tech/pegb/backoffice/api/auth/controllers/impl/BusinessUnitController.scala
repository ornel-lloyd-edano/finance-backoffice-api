package tech.pegb.backoffice.api.auth.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.JsValue
import play.api.mvc._
import tech.pegb.backoffice.api.auth.controllers.{BusinessUnitController ⇒ BusinessUnitControllerTrait}
import tech.pegb.backoffice.api.auth.dto.{BusinessUnitToCreate, BusinessUnitToUpdate}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.domain.auth.abstraction.BusinessUnitService
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.businessunit.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessUnitController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    businessUnitService: BusinessUnitService,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders with ConfigurationHeaders with BusinessUnitControllerTrait {

  import BusinessUnitController._
  import tech.pegb.backoffice.api.ApiErrors._
  import ApiController._
  import RequiredHeaders._

  private implicit val ec: ExecutionContext = executionContexts.genericOperations

  def create(reactivateIfExisting: Option[Boolean]): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      dto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUnitToCreate], isDeserializationStrict)
          .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to create business_unit. A mandatory field might be missing or its value is of wrong type.".toOption)))

      result ← EitherT(businessUnitService.create(
        dto.asDomain(getRequestFrom, getRequestDate),
        reactivateIfExisting.getOrElse(false))).leftMap(_.asApiError())

    } yield {
      result.asApi.toJsonStr

    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def findById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val getByIdCriteria = (Option(id), None).asDomain

    val futureResult = businessUnitService.getAllActiveBusinessUnits(getByIdCriteria, Seq.empty, None, None)
      .map(_.fold(_.asApiError().toLeft, _.headOption match {
        case Some(found) ⇒ found.asApi.toJsonStr.toRight
        case None ⇒ s"Business unit with id [$id] was not found.".asNotFoundApiError.toLeft
      }))
    futureResult.map(handleApiResponse(_))
  }

  def findAll(orderBy: Option[String], maybeLimit: Option[Int], maybeOffset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val findAllCriteria = (None, None).asDomain
    val latestVersionOrdering = "-updated_at".asDomain

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(validOrderBy).leftMap(_.log().asInvalidRequestApiError()))

      latestVersionResult ← EitherT(businessUnitService.getAllActiveBusinessUnits(
        findAllCriteria,
        latestVersionOrdering, Some(1), None)).leftMap(_.asApiError())

      total ← EitherT(executeIfGET(
        businessUnitService.countAllActiveBusinessUnits(findAllCriteria),
        NoCount.toFuture)).leftMap(_.asApiError())

      results ← EitherT(executeIfGET(
        businessUnitService.getAllActiveBusinessUnits(findAllCriteria, ordering, maybeLimit, maybeOffset),
        NoResult.toFuture)).leftMap(_.asApiError())

    } yield {
      (
        PaginatedResult(total, results = results.map(_.asApi), maybeLimit, maybeOffset).toJsonStr,
        latestVersionResult.headOption.flatMap(_.updatedAt.map(_.toString)))

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def update(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      dto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUnitToUpdate], isDeserializationStrict)
          .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to update business_unit. A mandatory field might be missing or its value is of wrong type.".toOption)))

      result ← EitherT(businessUnitService.update(id, dto.asDomain(getRequestFrom, getRequestDate)))
        .leftMap(_.asApiError())

    } yield {
      result.asApi.toJsonStr

    }).value.map(handleApiResponse(_))
  }

  def delete(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      dto ← EitherT.fromEither[Future](
        if (ctx.body.hasSomething) {
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to remove business_unit. A mandatory field might be missing or its value is of wrong type.".toOption))
        } else {
          Right(GenericRequestWithUpdatedAt.empty)
        })
      result ← EitherT(businessUnitService.remove(id, (getRequestFrom, getRequestDate, dto.lastUpdatedAt).asDomain))
        .leftMap(_.asApiError())

    } yield {
      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      id //result
    }).value.map(handleApiResponse(_)) //handleApiNoContentResponse

  }
}

object BusinessUnitController {
  val validOrderBy = Set("id", "name", "created_at", "updated_at")
}
