package tech.pegb.backoffice.api.parameter

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.parameter.dto.MetadataToRead
import tech.pegb.backoffice.api.swagger.model.{ParameterPaginatedResults, ParameterToRead}

@ImplementedBy(classOf[controllers.ParameterController])
@Api(value = "Parameters", produces = "application/json", consumes = "application/json")
trait ParameterController extends Routable {
  def getRoute: String = "parameters"
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[ParameterToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.parameter.dto.ParameterToCreate",
      example = "", //not showing correctly
      paramType = "body",
      name = "ParameterToCreate")))
  def createParameter: Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ParameterToRead], message = "")))
  def getParameterById(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ParameterPaginatedResults], message = "")))
  def getParametersByCriteria(
    key: Option[String],
    metadataId: Option[String],
    platforms: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[MetadataToRead], message = "")))
  def getMetadataById(id: String): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[MetadataToRead]], message = "")))
  def getMetadata: Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[ParameterToRead]], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.parameter.dto.ParameterToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "ParameterToUpdate")))
  def updateParameter(id: UUID): Action[String]

}
