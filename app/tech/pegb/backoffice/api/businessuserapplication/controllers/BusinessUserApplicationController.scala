package tech.pegb.backoffice.api.businessuserapplication.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MultipartFormData}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.businessuserapplication.controllers.impl.{BusinessUserApplicationController ⇒ BusinessUserApplicationControllerImpl}
import tech.pegb.backoffice.api.businessuserapplication.dto._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationPaginatedResult

@Api(value = "BusinessUserApplication", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[BusinessUserApplicationControllerImpl])
trait BusinessUserApplicationController extends Routable {

  override def getRoute: String = "business_user_applications"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationToCreateSwagger",
      example = "",
      paramType = "body",
      name = "BusinessUserApplicationToCreate")))
  def createBusinessUserApplication(id: UUID): Action[JsValue]

  @ApiOperation(value = "Returns list of business user applications")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationPaginatedResult], message = "")))
  def getBusinessUserApplication(
    businessName: Option[String],
    brandName: Option[String],
    businessCategory: Option[String],
    stage: Option[String],
    status: Option[String],
    phoneNumber: Option[String],
    email: Option[String],
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationToRead], message = "")))
  def getBusinessUserApplicationStageData(
    id: UUID,
    stage: String,
    status: Option[String]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationToRead], message = "")))
  def getBusinessUserApplicationById(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def submitBusinessUserApplication(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def approveBusinessUserApplication(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationExplanationToUpdateSwagger",
      example = "",
      paramType = "body",
      name = "BusinessUserApplicationExplanationToUpdate")))
  def cancelBusinessUserApplication(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationExplanationToUpdateSwagger",
      example = "",
      paramType = "body",
      name = "BusinessUserApplicationExplanationToUpdate")))
  def rejectBusinessUserApplication(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationExplanationToUpdateSwagger",
      example = "",
      paramType = "body",
      name = "BusinessUserApplicationExplanationToUpdate")))
  def sendForCorrectionBusinessUserApplication(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationConfigToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationConfigToCreateSwagger",
      example = "",
      paramType = "body",
      name = "BusinessUserApplicationConfigToCreateSwagger")))
  def createBusinessUserApplicationConfig(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationConfigToRead], message = "")))
  def getBusinessUserApplicationConfig(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationContactInfoToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.BusinessUserApplicationContactInfoToCreate",
      example = "",
      paramType = "body",
      name = "BusinessUserApplicationContactInfoToCreate")))
  def createBusinessUserApplicationContactInfo(id: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationContactInfoToRead], message = "")))
  def getBusinessUserApplicationContactInfo(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[BusinessUserApplicationDocumentToRead], message = "")))
  def getBusinessUserApplicationDocuments(id: UUID): Action[AnyContent]

  @ApiOperation(
    nickname = "businessUserApplicationDocumentUpload",
    value = "business user application document upload",
    notes = "business used application document upload",
    httpMethod = "POST",
    consumes = "multipart/form-data", produces = "multipart/form-data")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.DocumentMetadataToCreate",
      example = "",
      paramType = "form",
      name = "json"),
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "java.io.File",
      paramType = "form",
      name = "docfile")))
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SimpleDocumentToRead], message = "")))
  def createBusinessUserApplicationDocument(id: UUID): Action[MultipartFormData[TemporaryFile]]

}
