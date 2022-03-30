package tech.pegb.backoffice.api.makerchecker

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation, ApiResponse, ApiResponses}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.makerchecker.dto._
import tech.pegb.backoffice.api.swagger.model.TaskToReadPaginatedResults

@Api(value = "Tasks", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controller.MakerCheckerMgmtController])
trait MakerCheckerMgmtController extends Routable {
  def getRoute = "tasks"

  @ApiOperation(value = "Get a task and show its details")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TaskDetailToRead], message = "")))
  def getTask(id: UUID): Action[AnyContent]

  @ApiOperation(value = "Show tasks with option to filter")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TaskToReadPaginatedResults], message = "")))
  def getTasksByCriteria(
    moduleName: Option[String],
    status: Option[String],
    createdAtDateFrom: Option[LocalDateTimeFrom],
    createdAtDateTo: Option[LocalDateTimeTo],
    isReadOnly: Option[Boolean],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int] = None,
    offset: Option[Int] = None): Action[AnyContent]

  @ApiOperation(value = "Approve a pending task")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TaskToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ApproveTaskRequest",
      example = "",
      paramType = "body",
      name = "Approve Task")))
  def approveTask(id: UUID): Action[String]

  @ApiOperation(value = "Reject a pending task")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TaskToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.RejectTaskRequest",
      example = "",
      paramType = "body",
      name = "Reject Task")))
  def rejectTask(id: UUID): Action[String]

  @ApiOperation(value = "Create a pending task")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TaskToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.TaskToCreate",
      example = "",
      paramType = "body",
      name = "Create Task")))
  def createTask: Action[JsValue]

}
