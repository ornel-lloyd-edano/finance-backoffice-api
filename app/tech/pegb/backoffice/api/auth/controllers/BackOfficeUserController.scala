package tech.pegb.backoffice.api.auth.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.auth.controllers.impl.{BackOfficeUserController ⇒ BackOfficeUserControllerImpl}
import tech.pegb.backoffice.api.auth.dto.BackOfficeUserToRead
import tech.pegb.backoffice.api.swagger.model.BackOfficeUserPaginatedResult

@ImplementedBy(classOf[BackOfficeUserControllerImpl])
@Api(value = "BackOfficeUser", produces = "application/json", consumes = "application/json")
trait BackOfficeUserController extends Routable {
  def getRoute: String = "back_office_users"
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[BackOfficeUserToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BackOfficeUserToCreate",
      example = "", //not showing correctly
      paramType = "body",
      name = "BackOfficeUserToCreate")))
  def createBackOfficeUser(reactivate: Option[Boolean]): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BackOfficeUserToRead], message = "")))
  def getBackOfficeUserById(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BackOfficeUserPaginatedResult], message = "")))
  def getBackOfficeUsers(userName: Option[String], firstName: Option[String], lastName: Option[String],
    email: Option[String], phoneNumber: Option[String], roleId: Option[String],
    businessUnitId: Option[String], scopeId: Option[String], partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BackOfficeUserToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BackOfficeUserToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "BackOfficeUserToUpdate")))
  def updateBackOfficeUser(id: UUID): Action[JsValue]

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
  def deleteBackOfficeUser(id: UUID): Action[String]

}
