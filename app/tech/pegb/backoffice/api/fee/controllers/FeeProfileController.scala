package tech.pegb.backoffice.api.fee.controllers

import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.fee.Constants
import tech.pegb.backoffice.api.fee.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.fee.abstraction.FeeProfileManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.fee.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.fee.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class FeeProfileController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    feeProfileManagement: FeeProfileManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with api.fee.FeeProfileController {

  import ApiController._
  import FeeProfileController._
  import RequiredHeaders._

  implicit val executionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout = appConfig.FutureTimeout

  def createFeeProfile: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC
    val doneBy = getRequestFrom

    (for {
      feeProfileToCreateApi ← EitherT.fromEither[Future](ctx.body.as(classOf[FeeProfileToCreate], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError(MalformedCreateFeeProfileErrorMsg.some)))

      feeToCreate ← EitherT.fromEither[Future](feeProfileToCreateApi.asDomain(doneAt, doneBy)
        .toEither.leftMap(_.log().asInvalidRequestApiError("could not parse create request to domain".some)))

      createdProfile ← EitherT(feeProfileManagement.createFeeProfile(feeToCreate).map(_.map(_.asApiDetails.toJsonStr)
        .leftMap(_.asApiError())))

    } yield createdProfile).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getFeeProfile(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    feeProfileManagement.getFeeProfile(id).map(_.map(_.asApiDetails.toJsonStr)
      .leftMap(_.asApiError())).map(handleApiResponse(_))
  }

  def getFeeProfileByCriteria(
    id: Option[UUIDLike] = None,
    feeType: Option[String] = None,
    userType: Option[String] = None,
    tier: Option[String] = None,
    subscriptionType: Option[String] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    otherParty: Option[String] = None,
    instrument: Option[String] = None,
    calculationMethod: Option[String] = None,
    currencyCode: Option[String] = None,
    feeMethod: Option[String] = None,
    taxIncluded: Option[String] = None,
    partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      partialMatchSet ← EitherT.fromEither[Future](
        partialMatch.validatePartialMatch(Constants.feeProfilePartialMatchFields).leftMap(_.asMalformedRequestApiError()))

      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(Constants.feeProfileValidSorter).map(_.map(o ⇒
          o.copy(field = Constants.feeProfilesApiToTableColumnMapping(o.field)))).leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (id, feeType, userType, tier, subscriptionType, transactionType, channel, otherParty, instrument, calculationMethod,
          currencyCode, feeMethod, taxIncluded, partialMatchSet).asDomain.toEither.leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(feeProfileManagement.countFeeProfileByCriteria(criteria)
        .futureWithTimeout.map(_.leftMap(_.asApiError("failed to get fee profile count".some))), NoCount.toFuture))

      result ← EitherT(executeIfGET(
        feeProfileManagement.getFeeProfileByCriteria(criteria, ordering, limit, offset)
          .futureWithTimeout.map(_.leftMap(_.asApiError("failed to get fee profiles by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed to get latest version".some))))

    } yield (PaginatedResult(count, result.map(_.asApi), limit, offset).toJsonStr, latestVersion)).value.map(_.toTuple2FirstOneEither).map {
      case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
    }
  }

  def updateFeeProfile(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC
    val doneBy = getRequestFrom

    (for {
      feeToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[FeeProfileToUpdate], isDeserializationStrict).toEither
        .leftMap(_.log().asMalformedRequestApiError(MalformedUpdateFeeProfileErrorMsg.some)))

      feeToUpdate ← EitherT.fromEither[Future](feeToUpdateApiDto.asDomain(doneAt, doneBy).toEither
        .leftMap(_.log().asInvalidRequestApiError()))

      updatedFeeProfile ← EitherT(
        feeProfileManagement.updateFeeProfile(id, feeToUpdate).map(_.map(_.asApiDetails.toJsonStr)
          .leftMap(_.asApiError())))

    } yield updatedFeeProfile).value.map(handleApiResponse(_))
  }

  def deleteFeeProfile(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC
    val doneBy = getRequestFrom

    (for {
      feeToUpdate ← EitherT.fromEither[Future](
        ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict).toEither
          .leftMap(_.log().asMalformedRequestApiError()))

      deletedProfile ← EitherT(feeProfileManagement.deleteFeeProfile(id, doneBy, doneAt, feeToUpdate.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
    } yield deletedProfile).value.map(handleApiResponse(_))
  }

}

object FeeProfileController {

  val MalformedCreateFeeProfileErrorMsg = "Malformed request to create a fee profile. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateFeeProfileErrorMsg = "Malformed request to update a fee profile. Mandatory field is missing or value of a field is of wrong type."

}
