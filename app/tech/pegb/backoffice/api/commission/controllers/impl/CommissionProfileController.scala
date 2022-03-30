package tech.pegb.backoffice.api.commission.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.commission.dto.CommissionProfileToCreate
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.commission.abstraction.CommissionProfileManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.commission.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.commission.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.backoffice.api.RequiredHeaders._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommissionProfileController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService, //TODO: check latestVersion in TransactionConfigManagement
    commissionProfileManagement: CommissionProfileManagement,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with api.commission.controllers.CommissionProfileController {

  import ApiController._
  import CommissionProfileController._
  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout = appConfig.FutureTimeout

  def createCommissionProfile: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](
        ctx.body.as(classOf[CommissionProfileToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateCommissionProfileErrorMsg.some)))

      domainDto ← EitherT.fromEither[Future](
        apiDto.asDomain(requestId, doneAt, doneBy).toEither
          .leftMap(t ⇒ {
            logger.error("[createCommissionProfile] Error encountered while creating domain entity", t)
            t.asInvalidRequestApiError(t.getMessage.some)
          }))

      createdApplication ← EitherT(commissionProfileManagement.createCommissionProfile(domainDto))
        .map(_.asApiDetails.toJsonStr)
        .leftMap(_.asApiError())

    } yield createdApplication).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getCommissionProfile(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    commissionProfileManagement.getCommissionProfile(id).map(_.map(_.asApiDetails.toJsonStr)
      .leftMap(_.asApiError())).map(handleApiResponse(_))
  }

  def getCommissionProfileByCriteria(
    id: Option[UUIDLike],
    businessType: Option[String],
    tier: Option[String],
    subscriptionType: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    instrument: Option[String],
    currency: Option[String],
    calculationMethod: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {

      partialMatchSet ← EitherT.fromEither[Future](
        partialMatch.validatePartialMatch(CommissionProfilePartialMatchFields).leftMap(_.asMalformedRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(CommissionProfileValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (id, businessType, tier, subscriptionType, transactionType, currency, channel, instrument,
          calculationMethod, partialMatchSet).asDomain
          .toEither.leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(commissionProfileManagement.countCommissionProfileByCriteria(criteria)
        .futureWithTimeout.map(_.leftMap(_.asApiError("failed to get commission profile count".some))), NoCount.toFuture))

      result ← EitherT(executeIfGET(
        commissionProfileManagement.getCommissionProfileByCriteria(criteria, ordering, limit, offset)
          .futureWithTimeout.map(_.leftMap(_.asApiError("failed to get commission profiles by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed to get latest version".some))))

    } yield {
      (PaginatedResult(count, result.map(_.asApi), limit, offset).toJsonStr, latestVersion)
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
    }

  }

  def updateCommissionProfile(id: UUID): Action[String] = ???

  def deleteCommissionProfile(id: UUID): Action[String] = ???
}

object CommissionProfileController {

  val MalformedCreateCommissionProfileErrorMsg = "Malformed request to create. Mandatory field is missing or value of a field is of wrong type."

  val CommissionProfilePartialMatchFields = Set("disabled", "id", "other_party")

  val CommissionProfileValidSorter = Set(
    "uuid", "business_type", "tier", "subscription_type", "transaction_type",
    "channel", "instrument", "currency", "calculation_method")
}
