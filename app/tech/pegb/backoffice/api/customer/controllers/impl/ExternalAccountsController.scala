package tech.pegb.backoffice.api.customer.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.customer.controllers.impl.BusinessUserController.{externalAccountValidPartialMatch, externalAccountValidSorter}
import tech.pegb.backoffice.api.customer.dto.{ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.api.error.jackson.Implicits._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ExternalAccountsController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    externalAccountMgmt: ExternalAccountManagement,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders with controllers.ExternalAccountsController {

  import ApiController._
  import tech.pegb.backoffice.api.ApiErrors._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.genericOperations

  def getExternalAccount(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val criteria = (Option(id.toUUIDLike), None).asDomain
    externalAccountMgmt.getExternalAccountByCriteria(criteria, Nil, None, None)
      .map(_.fold(_.asApiError().toLeft, _.headOption match {
        case Some(externalAccount) ⇒
          externalAccount.asApi.toJsonStr.toRight
        case None ⇒ s"External account with id [$id] was not found".asNotFoundApiError.toLeft
      }))
      .map(handleApiResponse(_))
  }

  def getExternalAccountsByCriteria(
    customerId: Option[UUIDLike],
    customerName: Option[String],
    currency: Option[String],
    providerName: Option[String],
    accountNumber: Option[String],
    accountHolder: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      limit ← EitherT.fromEither[Future](Try(limit.flatMap(lim ⇒ Math.min(lim, appConfig.PaginationMaxLimit).some)
        .orElse(appConfig.PaginationLimit.some)).toEither
        .leftMap(_ ⇒ "Unexpected error during resolution of limit".asUnknownApiError))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(externalAccountValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(externalAccountValidPartialMatch)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((None, customerId, providerName,
        accountHolder, accountNumber, currency, partialMatchFields).asDomain.toRight)

      total ← EitherT(executeIfGET(externalAccountMgmt.count(criteria), NoCount.toFuture)).leftMap(_.asApiError())

      latestVersion ← EitherT(externalAccountMgmt.getLatestVersion(criteria)).leftMap(_.asApiError())

      results ← EitherT(executeIfGET(
        externalAccountMgmt.getExternalAccountByCriteria(criteria, ordering, limit, offset),
        NoResult.toFuture)).leftMap(_.asApiError())

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersion.flatMap(_.updatedAt.map(_.toString)))
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def createExternalAccount: Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[ExternalAccountToCreate], isDeserializationStrict)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      domainDto ← EitherT.fromEither[Future](apiDto.asDomain(requestId, doneBy, doneAt).toRight)

      result ← EitherT(externalAccountMgmt.createExternalAccount(domainDto))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateExternalAccount(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    val criteria = (Option(id.toUUIDLike), None).asDomain
    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[ExternalAccountToUpdate], isStrict = false)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      domainDto ← EitherT.fromEither[Future](apiDto.asDomain(doneBy, doneAt).toRight)

      result ← EitherT(externalAccountMgmt.updateExternalAccount(criteria, domainDto))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_))
  }

  def deleteExternalAccount(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    val criteria = (Option(id.toUUIDLike), None).asDomain
    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isStrict = false)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      result ← EitherT(externalAccountMgmt.deleteExternalAccount(criteria, apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .map(_ ⇒ s"""{"id":"$id","status":"deleted"}""")
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_))
  }
}
