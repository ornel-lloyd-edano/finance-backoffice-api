package tech.pegb.backoffice.api.i18n

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.i18n.dto.{I18nStringBulkResponse, I18nStringToRead}
import tech.pegb.backoffice.api.swagger.model.{I18nDictionaryResponse, I18nStringPaginatedResult}

@Api(value = "I18n String", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.I18nStringController])
trait I18nStringController extends Routable {
  override def getRoute: String = "strings"
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[I18nStringToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.I18nStringToCreate",
      example = "",
      paramType = "body",
      name = "I18nStringToCreate")))
  def createI18nString: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[I18nStringBulkResponse], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.I18nStringBulkCreate",
      example = "",
      paramType = "body",
      name = "I18nStringToCreate")))
  def bulkI18nStringCreate: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[I18nStringToRead], message = "")))
  def getI8nStringById(id: Int): Action[AnyContent]

  @ApiOperation(value = "Returns list of i18n string")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[I18nStringPaginatedResult], message = "")))
  def getI18nString(
    id: Option[Int],
    key: Option[String],
    locale: Option[String],
    platform: Option[String],
    `type`: Option[String],
    explanation: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[I18nStringToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.I18nStringToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "I18nStringToUpdate")))
  def updateI18nString(id: Int): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Int], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteI18nString(id: Int): Action[String]

  @ApiOperation(value = "Returns i18n dictionary for certain platform and locale")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[I18nDictionaryResponse], message = "")))
  def getI18nDictionary(platform: Option[String]): Action[AnyContent]
}
