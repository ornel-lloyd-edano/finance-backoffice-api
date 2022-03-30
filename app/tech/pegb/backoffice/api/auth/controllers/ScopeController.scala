package tech.pegb.backoffice.api.auth.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.auth.dto.ScopeToRead
import tech.pegb.backoffice.api.swagger.model.ScopePaginatedResult

@Api(value = "Scope", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.ScopeController])
trait ScopeController extends Routable {
  def getRoute: String = "scopes"
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ScopeToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ScopeToCreate",
      example = "",
      paramType = "body",
      name = "ScopeToCreate")))
  def createScope(reactivate: Option[Boolean]): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ScopeToRead], message = "")))
  def getScopeById(id: UUID): Action[AnyContent]

  @ApiOperation(value = "Returns list of scopes")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ScopePaginatedResult], message = "")))
  def getAllScopes(orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ScopeToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ScopeToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "ScopeToUpdate")))
  def updateScopeById(id: UUID): Action[String]

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
  def deleteScopeById(id: UUID): Action[String]

}
