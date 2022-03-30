package tech.pegb.backoffice.api.notification

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.notification.controllers.NotificationMgmtController
import tech.pegb.backoffice.api.notification.dto.{NotificationTemplateToRead, NotificationToRead}
import tech.pegb.backoffice.api.swagger.model.{NotificationPaginatedResults, NotificationTemplatePaginatedResults}
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Notification Management", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[NotificationMgmtController])
trait NotificationManagementController extends Routable {
  def getRoute = "notification_templates"

  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[NotificationTemplateToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.NotificationTemplateToCreate",
      example = "", //not showing correctly
      paramType = "body",
      name = "NotificationToCreate")))
  def createNotificationTemplate: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationTemplateToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deactivateNotificationTemplate(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationTemplateToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def activateNotificationTemplate(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationTemplateToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.NotificationTemplateToUpdate",
      example = "",
      paramType = "body",
      name = "NotificationTemplateToUpdate")))
  def updateNotificationTemplate(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationTemplatePaginatedResults], message = "")))
  def getNotificationTemplatesByCriteria(
    id: Option[UUIDLike],
    name: Option[String],
    channel: Option[String],
    createdAtFrom: Option[LocalDateTimeFrom],
    createdAtTo: Option[LocalDateTimeTo],
    isActive: Option[Boolean] = None,
    partialMatchFields: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationPaginatedResults], message = "")))
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
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationTemplateToRead], message = "")))
  def getNotificationTemplateById(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[NotificationToRead], message = "")))
  def getNotificationById(id: UUID): Action[AnyContent]
}
