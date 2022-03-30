package tech.pegb.backoffice.api.notification.controllers

import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc._
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model._
import tech.pegb.backoffice.api.notification.dto.{NotificationTemplateToCreate, NotificationTemplateToUpdate}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.notification.abstraction.NotificationManagementService
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.notification.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.notification.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class NotificationMgmtController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    notifMgmtService: NotificationManagementService,
    implicit val appConfig: AppConfig)

  extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with api.notification.NotificationManagementController {

  import ApiController._
  import ApiErrors._
  import NotificationMgmtController._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout
  val PaginationMaxCap: Int = appConfig.PaginationMaxLimit

  def createNotificationTemplate: Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      dto ← EitherT.fromEither[Future](ctx.body.toString.as(classOf[NotificationTemplateToCreate], isStrict = false)
        .toEither.leftMap(_.asApiError("malformed json to create notification template".some)))

      notificationTemplate ← EitherT(notifMgmtService.createNotificationTemplate(dto.asDomain(doneAt, doneBy))
        .map(_.leftMap(_.asApiError())))
    } yield notificationTemplate.asApi.toJsonStr).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def deactivateNotificationTemplate(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    implicit val requestId: UUID = getRequestId

    (for {
      request ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.asApiError("malformed json to update notification template".some)))

      unit ← EitherT(
        notifMgmtService.deactivateNotificationTemplate(
          id = id,
          doneAt = doneAt.toLocalDateTimeUTC,
          doneBy = doneBy,
          lastUpdatedAt = request.lastUpdatedAt.map(_.toLocalDateTimeUTC))
          .map(_.leftMap(_.asApiError())))
    } yield unit).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def activateNotificationTemplate(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    implicit val requestId: UUID = getRequestId

    (for {
      request ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.asApiError("malformed json to update notification template".some)))

      unit ← EitherT(
        notifMgmtService.activateNotificationTemplate(
          id = id,
          doneAt = doneAt.toLocalDateTimeUTC,
          doneBy = doneBy,
          lastUpdatedAt = request.lastUpdatedAt.map(_.toLocalDateTimeUTC))
          .map(_.leftMap(_.asApiError())))
    } yield unit).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def updateNotificationTemplate(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    implicit val requestId: UUID = getRequestId

    (for {
      dto ← EitherT.fromEither[Future](ctx.body.toString.as(classOf[NotificationTemplateToUpdate], isStrict = false)
        .toEither.leftMap(_.asApiError("malformed json to update notification template".some)))

      unit ← EitherT(notifMgmtService.updateNotificationTemplate(id, dto.asDomain(doneAt, doneBy))
        .map(_.leftMap(_.asApiError())))
    } yield unit).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def getNotificationTemplatesByCriteria(
    id: Option[UUIDLike],
    name: Option[String],
    channel: Option[String],
    createdAtFrom: Option[LocalDateTimeFrom],
    createdAtTo: Option[LocalDateTimeTo],
    isActive: Option[Boolean],
    partialMatchFields: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      _ ← EitherT.fromEither[Future]((createdAtFrom, createdAtTo, limit) match {
        case (Some(from), Some(to), _) if from.localDateTime.isAfter(to.localDateTime) ⇒

          ApiError(requestId, ApiErrorCodes.InvalidRequest, "created_at_from must be before or equal to created_at_to").toLeft
        case (_, _, Some(lim)) if lim > PaginationMaxCap ⇒

          ApiError(requestId, ApiErrorCodes.InvalidRequest, s"Limit provided(${limit.get}) is greater than max configured value of $PaginationMaxCap").toLeft

        case _ ⇒ Right(())
      })

      ordering ← EitherT.fromEither[Future](orderBy.validateOrderBy(validNotificationTemplateOrderByFields)
        .map(_.mkString(",").asDomain)
        .leftMap(_.log().asInvalidRequestApiError()))

      validatedPartialMatchFields ← EitherT.fromEither[Future](partialMatchFields.validatePartialMatch(validNotificationTemplatePartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = (id, name, channel, createdAtFrom, createdAtTo, isActive, partialMatchFields).asDomain
        .copy(partialMatchFields = validatedPartialMatchFields)

      count ← EitherT(executeIfGET(notifMgmtService.countNotificationTemplatesByCriteria(criteria.toOption)
        .futureWithTimeout.map(_.leftMap(_.asApiError("failed getting the count of notification templates".some))), NoCount.toFuture))

      notificationTemplates ← EitherT(executeIfGET(notifMgmtService.getNotificationTemplatesByCriteria(criteria.toOption, ordering, limit, offset)
        .map(_.leftMap(_.asApiError("failed getting the notification templates by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed getting the latest version of notification templates".some))))

    } yield (PaginatedResult(total = count, results = notificationTemplates.map(_.asApi), limit = limit, offset = offset).toJsonStrWithoutEscape, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, versionHeader) ⇒ handleApiResponse(result).withLatestVersionHeader(versionHeader)
      }
  }

  def getNotificationsByCriteria(
    id: Option[UUIDLike],
    templateId: Option[UUIDLike],
    userId: Option[UUIDLike],
    operationId: Option[String],
    channel: Option[String],
    title: Option[String],
    content: Option[String],
    address: Option[String],
    status: Option[String],
    createdAtFrom: Option[LocalDateTimeFrom],
    createdAtTo: Option[LocalDateTimeTo],
    partialMatchFields: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      _ ← EitherT.fromEither[Future] {
        (createdAtFrom, createdAtTo, limit) match {
          case (Some(from), Some(to), _) if from.localDateTime.isAfter(to.localDateTime) ⇒

            ApiError(requestId, ApiErrorCodes.InvalidRequest, "created_at_from must be before or equal to created_at_to").toLeft
          case (_, _, Some(lim)) if lim > PaginationMaxCap ⇒

            ApiError(requestId, ApiErrorCodes.InvalidRequest, s"Limit provided(${limit.get}) is greater than max configured value of $PaginationMaxCap").toLeft

          case _ ⇒ ().toRight
        }
      }

      ordering ← EitherT.fromEither[Future](orderBy.validateOrderBy(validNotificationOrderByFields)
        .map(_.mkString(",").asDomain)
        .leftMap(_.log().asInvalidRequestApiError()))

      _ ← EitherT.fromEither[Future](partialMatchFields.validatePartialMatch(validNotificationPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria = (id, templateId, userId, operationId, channel, title, content,
        address, status, createdAtFrom, createdAtTo, partialMatchFields).asDomain

      count ← EitherT(executeIfGET(notifMgmtService.countNotificationsByCriteria(criteria.toOption)
        .futureWithTimeout.map(_.leftMap(_.asApiError("failed getting the count of notifications".some))), NoCount.toFuture))

      notifications ← EitherT(executeIfGET(notifMgmtService.getNotificationsByCriteria(criteria.toOption, ordering, limit, offset)
        .map(_.leftMap(_.asApiError("failed getting the notifications by criteria".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed getting the latest version of notifications".some))))

    } yield (PaginatedResult(total = count, results = notifications.map(_.asApi), limit = limit, offset = offset).toJsonStrWithoutEscape, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, versionHeader) ⇒ handleApiResponse(result).withLatestVersionHeader(versionHeader)
      }

  }

  def getNotificationTemplateById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    notifMgmtService.getNotificationTemplatesByCriteria(id.asNotificationTemplateCriteria.toOption, Nil, None, None)
      .map(_.fold(
        serviceError ⇒ Left(serviceError.asApiError(s"failed to get notification template by id $id".some)),
        notifications ⇒
          notifications.headOption.map(_.asApi.toJsonStrWithoutEscape)
            .toRight(ApiError(requestId, ApiErrorCodes.NotFound, s"notification template with id: $id not found"))))
      .map(handleApiResponse(_))
  }

  def getNotificationById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    notifMgmtService.getNotificationsByCriteria(id.asNotificationCriteria.toOption, Nil, None, None)
      .map(_.fold(
        serviceError ⇒ Left(serviceError.asApiError(s"failed to get notification by id $id".some)),
        notifications ⇒
          notifications.headOption.map(_.asApi.toJsonStr)
            .toRight(ApiError(requestId, ApiErrorCodes.NotFound, s"notification with id: $id not found"))))
      .map(handleApiResponse(_))
  }
}

object NotificationMgmtController {
  val validNotificationTemplatePartialMatchFields = Set("disabled", "id", "name", "channel")
  val validNotificationTemplateOrderByFields = Set("id", "name", "channel", "created_at")

  val validNotificationOrderByFields = Set("id", "template_id", "channel", "title", "user_id", "address",
    "operation_id", "created_at", "sent_at", "retries", "status")
  val validNotificationPartialMatchFields = Set("disabled", "id", "user_id", "address", "title", "content")
}
