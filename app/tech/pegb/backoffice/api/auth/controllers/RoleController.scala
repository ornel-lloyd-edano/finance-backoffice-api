package tech.pegb.backoffice.api.auth.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.auth.dto.RoleToRead
import tech.pegb.backoffice.api.swagger.model.RolePaginatedResult

@Api(value = "Role", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.RoleController])
trait RoleController extends Routable {
  def getRoute: String = "roles"
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[RoleToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.RoleToCreate",
      example = "", //not showing correctly
      paramType = "body",
      name = "RoleToCreate")))
  def createRole(reactivate: Option[Boolean]): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[RoleToRead], message = "")))
  def getRoleById(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[RolePaginatedResult], message = "")))
  def getRoles(orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[RoleToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.RoleToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "RoleToUpdate")))
  def updateRole(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 204, response = classOf[String], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteRole(id: UUID): Action[String]

}
