package tech.pegb.backoffice.api.auth.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.auth.dto.PermissionToRead
import tech.pegb.backoffice.api.swagger.model.PermissionPaginatedResult

@Api(value = "Permission", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.PermissionController])
trait PermissionController extends Routable {
  def getRoute: String = "permissions"
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[PermissionToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.PermissionToCreate",
      example = "",
      paramType = "body",
      name = "PermissionToCreate")))
  def createPermission(reactivate: Option[Boolean]): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[PermissionToRead], message = "")))
  def getPermissionById(id: UUID): Action[AnyContent]

  @ApiOperation(value = "Returns list of Permissions")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[PermissionPaginatedResult], message = "")))
  def getAllPermissions(
    businessUnitId: Option[UUID],
    roleId: Option[UUID],
    maybeUserId: Option[UUID],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[PermissionToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.PermissionToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "PermissionToUpdate")))
  def updatePermissionById(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[UUID], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deletePermissionById(id: UUID): Action[String]

}
