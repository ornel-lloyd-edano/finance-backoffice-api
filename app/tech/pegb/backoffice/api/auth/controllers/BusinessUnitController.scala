package tech.pegb.backoffice.api.auth.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.auth.controllers.impl.{BusinessUnitController ⇒ BusinessUnitControllerImpl}
import tech.pegb.backoffice.api.auth.dto.BusinessUnitToRead
import tech.pegb.backoffice.api.swagger.model.BusinessUnitPaginatedResult

@ImplementedBy(classOf[BusinessUnitControllerImpl])
@Api(value = "Business Unit", produces = "application/json", consumes = "application/json")
trait BusinessUnitController extends Routable {
  def getRoute: String = "business_units"
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[BusinessUnitToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUnitToCreateOrUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "BusinessUnitToCreateOrUpdate")))
  def create(reactivateIfExisting: Option[Boolean]): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUnitToRead], message = "")))
  def findById(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUnitPaginatedResult], message = "")))
  def findAll(orderBy: Option[String], maybeLimit: Option[Int], maybeOffset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUnitToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUnitToCreateOrUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "BusinessUnitToCreateOrUpdate")))
  def update(id: UUID): Action[JsValue]

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
  def delete(id: UUID): Action[String]
}
