package tech.pegb.backoffice.api.types.controllers

import java.util.UUID

import cats.implicits._
import com.google.inject.Inject
import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.types.dto.TypeToRead
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.domain.types.implementation.TypesService
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.types.Implicits.TypeAdapter
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Api(value = "Types", produces = "application/json", consumes = "application/json")
class TypesController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    implicit val appConfig: AppConfig,
    typesService: TypesService) extends ApiController(controllerComponents) with RequiredHeaders {

  //TODO, this was from PR comment but merging nevertheless, update later
  /*

    Swagger cannot read Map[String, Seq[TypeToRead]] properly, try creating a case class in swagger.model

    @ApiModel(value = "AllTypesToRead")

    case class TypesMapToRead(@ApiModelProperty(name = "{insert-desc-of-type-here}", required = true) typeToRead: Array[TypeToRead])

    and then in the annotation:

    @ApiOperation(value = "Returns all types")

    @ApiResponses( Array(new ApiResponse(code = 200, response = classOf[Array[TypesMapToRead]], message = "")))

   */
  @ApiOperation(value = "Returns all types")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Map[String, Seq[TypeToRead]]], message = "")))
  def getAllTypes: Action[AnyContent] = LoggedAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    handleApiResponse {
      typesService.fetchAllTypes.map(_.mapValues(_.map(_.asApi)).toJsonStr).leftMap(_.asApiError())
    }
  }

  //TODO Swagger cannot read Seq properly unless it is wrapped by case class, pls use Array
  @ApiOperation(value = "Returns specific type values")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Seq[TypeToRead]], message = "")))
  def getTypes(kind: String): Action[AnyContent] = LoggedAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    handleApiResponse {
      typesService.fetchCustomType(kind.sanitize).map(_.map(_.asApi).toJsonStr).leftMap(_.asApiError())
    }
  }

  @ApiOperation(value = "Returns account types")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Seq[TypeToRead]], message = "")))
  def getAccountTypes: Action[AnyContent] = LoggedAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    handleApiResponse {
      typesService.fetchAccountTypes.map(_.map(_.asApi).toJsonStr).leftMap(_.asApiError())
    }
  }

}
