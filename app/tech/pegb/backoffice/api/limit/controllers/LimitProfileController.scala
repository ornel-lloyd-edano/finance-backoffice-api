package tech.pegb.backoffice.api.limit.controllers

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.limit.Constants
import tech.pegb.backoffice.api.limit.dto.{LimitProfileToCreate, LimitProfileToUpdate}
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.limit.abstraction.LimitManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.limit.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.limit.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LimitProfileController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig,
    limitManagement: LimitManagement) extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with api.limit.LimitProfileController {

  import ApiController._
  import LimitProfileController._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def getLimitProfile(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    limitManagement.getLimitProfile(id)
      .map(_.map(_.asApiDetailed.toJsonStr).leftMap(_.asApiError()))
      .map(handleApiResponse(_))
  }

  def getLimitProfileByCriteria(
    id: Option[UUIDLike],
    limitType: Option[String],
    userType: Option[String],
    tier: Option[String],
    subscription: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    otherParty: Option[String],
    instrument: Option[String],
    interval: Option[String],
    currencyCode: Option[String],
    partialMatch: Option[String],
    orderByOption: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val orderBy = orderByOption.orElse("-created_at".some)

    (for {
      partialMatchSet ← EitherT.fromEither[Future](
        partialMatch.validatePartialMatch(Constants.limitProfilePartialMatchFields)
          .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(Constants.limitProfileValidSorter)
          .map(_.map(o ⇒ o.copy(field = Constants.limitProfilesApiToTableColumnMapping(o.field))))
          .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (id, limitType, userType, tier, subscription, transactionType, channel, otherParty, instrument,
          interval, currencyCode, partialMatchSet).asDomain
          .toEither.leftMap(_.log().asMalformedRequestApiError()))

      count ← EitherT(executeIfGET(limitManagement.countLimitProfileByCriteria(criteria).futureWithTimeout
        .map(_.leftMap(_.asApiError("failed getting the count of limit profiles".some))), NoCount.toFuture))

      limitProfiles ← EitherT(executeIfGET(limitManagement.getLimitProfileByCriteria(criteria, ordering, limit, offset)
        .futureWithTimeout
        .map(_.leftMap(_.asApiError("failed to get the limit profiles by provided criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed to get latest version for limit profiles".some))))

    } yield {

      (PaginatedResult(count, limitProfiles.map(_.asApi), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither)
      .map { case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version) }
  }

  def createLimitProfile: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate.toLocalDateTimeUTC
    val doneBy = getRequestFrom

    (for {
      limitProfileToCreateApi ← EitherT.fromEither[Future](ctx.body.as(
        classOf[LimitProfileToCreate],
        isDeserializationStrict).toEither.leftMap(_.log().asMalformedRequestApiError(MalformedCreateLimitProfileErrorMsg.some)))

      limitToCreate ← EitherT.fromEither[Future](limitProfileToCreateApi.asDomain(doneBy, doneAt)
        .toEither.leftMap(_.asInvalidRequestApiError()))

      createdLimitProfile ← EitherT(limitManagement.createLimitProfile(limitToCreate).map(_.map(_.asApiDetailed.toJsonStr)
        .leftMap(_.asApiError())))

    } yield createdLimitProfile).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateLimitProfile(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC
    val doneBy = getRequestFrom

    (for {
      limitProfileToUpdateApi ← EitherT.fromEither[Future](ctx.body.as(classOf[LimitProfileToUpdate], isDeserializationStrict).toEither
        .leftMap(_.log().asMalformedRequestApiError(MalformedUpdateLimitProfileErrorMsg.some)))

      limitToUpdate ← EitherT.fromEither[Future](limitProfileToUpdateApi.asDomain(doneBy, doneAt)
        .toEither.leftMap(_.log().asInvalidRequestApiError()))

      updatedLimit ← EitherT(limitManagement.updateLimitProfileValues(id, limitToUpdate)
        .map(_.map(_.asApiDetailed.toJsonStr).leftMap(_.asApiError())))

    } yield updatedLimit).value.map(handleApiResponse(_))
  }

  def deleteLimitProfile(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC
    val doneBy = getRequestFrom

    (for {
      limitToUpdate ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      deletedProfile ← EitherT(limitManagement.deleteLimitProfile(id, doneBy, doneAt, limitToUpdate.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.map(_.asApiDetailed.toJsonStr).leftMap(_.asApiError())))

    } yield deletedProfile).value.map(handleApiResponse(_))

  }
}

object LimitProfileController {

  val MalformedCreateLimitProfileErrorMsg = "Malformed request to create a limit profile. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateLimitProfileErrorMsg = "Malformed request to update a limit profile. Mandatory field is missing or value of a field is of wrong type."

}

