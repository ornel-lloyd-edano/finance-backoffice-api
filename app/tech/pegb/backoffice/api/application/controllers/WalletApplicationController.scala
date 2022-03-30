package tech.pegb.backoffice.api.application.controllers

import java.time.LocalDate
import java.util.UUID

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.application.dto.WalletApplicationToReject
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult}
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.application.Implicits._
import tech.pegb.backoffice.mapping.api.domain.document.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.application.Implicits._
import tech.pegb.backoffice.mapping.domain.api.document.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class WalletApplicationController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    documentsMgmt: DocumentManagement,
    walletApplicationManagement: WalletApplicationManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with api.application.WalletApplicationController {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def getWalletApplication(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    walletApplicationManagement.getWalletApplicationById(id)
      .map(result ⇒ handleApiResponse(result.map(_.asDetailApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getDocumentsByWalletApplication(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val criteria = id.asDomain

    (for {
      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of wallet application documents".asUnknownApiError)))

      results ← EitherT(executeIfGET(documentsMgmt.getDocumentsByCriteria(criteria, Nil, limit = None, offset = None)
        .map(_.leftMap(_ ⇒ "Failed fetching wallet application documents".asUnknownApiError)), NoResult.toFuture))

    } yield {

      (PaginatedResult(total = results.size, results = results.map(_.asApi), limit = None, offset = None).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  //TODO add NameAttribute if required
  def getWalletApplicationsByCriteria(
    status: Option[String],
    name: Option[String],
    fullName: Option[String],
    msisdn: Option[String],
    nationalId: Option[String],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    orderBy: Option[String],
    limit: Option[Int] = None,
    offset: Option[Int] = None): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId = getRequestId

    val validOrderBy = Set("stage", "msisdn", "status", "checked_at", "total_score", "fullname_score",
      "fullname_original", "nationality_original", "gender_original", "birthdate_original", "birthplace_original", "updated_at")

    val buildCriteria = (status, name, fullName, msisdn, nationalId, startDate, endDate).asDomain

    (for {

      ordering ← EitherT(orderBy.validateOrdering(validOrderBy).leftMap(_.log().asInvalidRequestApiError()).toFuture)

      criteria ← EitherT(buildCriteria.toEither.leftMap(_.log().asInvalidRequestApiError()).toFuture)

      total ← EitherT(executeIfGET(walletApplicationManagement.countWalletApplicationsByCriteria(criteria)
        .map(_.leftMap(_ ⇒ "Failed counting wallet applications".asUnknownApiError)), NoCount.toFuture)
        .futureWithTimeout
        .recover {
          case e: Throwable ⇒ Right(-1)
        })

      results ← EitherT(executeIfGET(walletApplicationManagement
        .getWalletApplicationsByCriteria(criteria, ordering, limit, offset)
        .map(_.leftMap(_ ⇒ "Failed fetching wallet applications".asUnknownApiError)), NoResult.toFuture))

      maybeLatestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of wallet applications".asUnknownApiError)))

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, maybeLatestVersion)
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  def approveWalletApplication(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate

    (for {
      requestWithUpdatedAt ← EitherT(ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log().asInvalidRequestApiError("Malformed request to approve wallet application".toOption)).toFuture)

      result ← EitherT(walletApplicationManagement.approvePendingWalletApplication(
        id,
        doneBy,
        doneAt.toLocalDateTimeUTC,
        requestWithUpdatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.bimap(_.asApiError(), _.asApi.toJsonStr)))

    } yield result).value.map(handleApiResponse(_))
  }

  def rejectWalletApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate

    (for {
      walletApplicationToReject ← EitherT(ctx.body.toString.as(
        classOf[WalletApplicationToReject],
        isDeserializationStrict).toEither.leftMap(
        _.log().asInvalidRequestApiError("Malformed request to reject wallet application".toOption)).toFuture)

      result ← EitherT(walletApplicationManagement.rejectPendingWalletApplication(
        id,
        doneBy,
        doneAt.toLocalDateTimeUTC,
        walletApplicationToReject.reason.sanitize,
        walletApplicationToReject.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
    } yield result).value.map(handleApiResponse(_))

  }
}
