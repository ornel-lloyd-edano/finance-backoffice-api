package tech.pegb.backoffice.api.i18n.controllers

import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.RequiredHeaders.RequiredResponseHeaders
import tech.pegb.backoffice.api.i18n.Constants
import tech.pegb.backoffice.api.i18n.dto.{I18nStringBulkCreate, I18nStringToCreate, I18nStringToUpdate}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.i18n.abstraction.I18nStringManagement
import tech.pegb.backoffice.domain.i18n.dto.I18nStringCriteria
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nLocale, I18nPlatform}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.i18n.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.i18n.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class I18nStringController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    i18nStringManagement: I18nStringManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with api.i18n.I18nStringController {

  import ApiController._
  import I18nStringController._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def createI18nString: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      i18nStringToCreateApi ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[I18nStringToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateI18NProfileErrorMsg.some)))

      i18nStringToCreate ← EitherT.fromEither[Future](
        i18nStringToCreateApi.asDomain(doneAt).toEither.leftMap(_.asInvalidRequestApiError()))

      createdI18N ← EitherT(i18nStringManagement.createI18nString(i18nStringToCreate)).map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())

    } yield createdI18N).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def bulkI18nStringCreate: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      i18nStringBulkCreateApi ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[I18nStringBulkCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedBulkCreateI18NProfileErrorMsg.some)))

      domainLocale ← EitherT.fromEither[Future](
        i18nStringBulkCreateApi.getDomainLocale.toEither.leftMap(_.asInvalidRequestApiError()))

      domainCreateDto ← EitherT.fromEither[Future](
        i18nStringBulkCreateApi.getDomainCreateDtos(doneAt).toEither.leftMap(_.asInvalidRequestApiError()))

      bulkResult ← EitherT(i18nStringManagement.bulkCreateI18nString(domainLocale, domainCreateDto)).map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())

    } yield bulkResult).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getI8nStringById(id: Int): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    i18nStringManagement.getI18nStringById(id).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi.toJsonStrWithoutEscape).leftMap(_.asApiError())))
  }

  def getI18nString(
    id: Option[Int],
    key: Option[String],
    locale: Option[String],
    platform: Option[String],
    `type`: Option[String],
    explanation: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      partialMatchSet ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(Constants.i18nStringPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(Constants.i18nStringValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (id, key, locale, platform, `type`, explanation, partialMatchSet).asDomain.toEither
          .leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(i18nStringManagement.countI18nStringByCriteria(criteria)
        .futureWithTimeout
        .map(_.leftMap(_.asApiError("failed to get the count of I18N string".some))), NoCount.toFuture))

      i18nStrings ← EitherT(executeIfGET(
        i18nStringManagement.getI18nStringByCriteria(criteria, ordering, limit, offset)
          .futureWithTimeout
          .map(_.leftMap(_.asApiError("failed to get the list of I18N string".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed to get the latest version".some))))

    } yield (PaginatedResult(count, i18nStrings.map(_.asApi), limit, offset).toJsonStrWithoutEscape, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
      }
  }

  def updateI18nString(id: Int): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate

    (for {
      i18nStringToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.toString.as(
        classOf[I18nStringToUpdate],
        isDeserializationStrict).toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateI18NProfileErrorMsg.some)))

      i18nStringToUpdate ← EitherT.fromEither[Future](
        i18nStringToUpdateApiDto.asDomain(doneAt).toEither.leftMap(_.asInvalidRequestApiError()))

      updatedI18nString ← EitherT(i18nStringManagement.updateI18nString(id, i18nStringToUpdate).map(_.map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())))

    } yield updatedI18nString).value.map(handleApiResponse(_))
  }

  def deleteI18nString(id: Int): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      deleteWithUpdatedAt ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      deletedString ← EitherT(i18nStringManagement
        .deleteI18nString(id, deleteWithUpdatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC)).map(_.map(_.toJsonStr)
          .leftMap(_.asApiError())))

    } yield deletedString).value.map(handleApiResponse(_))
  }

  def getI18nDictionary(platform: Option[String]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val rawLocale = ctx.headers.get(ACCEPT_LANGUAGE)

    val i18nLocale = I18nLocale(rawLocale.flatMap(_.toSeqByComma.headOption).getOrElse(appConfig.I18N.i18nLocale))
    val i18nPlatform = I18nPlatform(platform.getOrElse(appConfig.I18N.i18nPlatform))
    val criteria = I18nStringCriteria(locale = i18nLocale.some, platform = i18nPlatform.some)

    (for {
      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed to get the latest version".some))))

      i18nPairs ← EitherT(executeIfGET(i18nStringManagement.getI18nDictionary(criteria)
        .futureWithTimeout
        .map(_.leftMap(_.asApiError("failed to get I18N Dictionary".some))), NoResult.toFuture))
    } yield {
      (i18nPairs.map(_.asApi.toJsonKeyValString).mkString("{", ",", "}"), latestVersion)
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
    }
  }
}

object I18nStringController {

  val MalformedCreateI18NProfileErrorMsg = "Malformed request to create a I18n string. Mandatory field is missing or value of a field is of wrong type."
  val MalformedBulkCreateI18NProfileErrorMsg = "Malformed request for bulk create of I18n string. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateI18NProfileErrorMsg = "Malformed request to update a I18n string. Mandatory field is missing or value of a field is of wrong type."

}
