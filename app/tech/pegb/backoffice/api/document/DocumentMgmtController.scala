package tech.pegb.backoffice.api.document

import java.time.LocalDate
import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.Files
import play.api.mvc.{Action, AnyContent, MultipartFormData}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.util.UUIDLike

@ImplementedBy(classOf[controllers.DocumentMgmtController])
@Api(value = "Documents", produces = "application/json", consumes = "application/json")
trait DocumentMgmtController extends Routable {

  def getRoute: String = "documents"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  def getDocument(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[Byte]], message = "")))
  def getDocumentFile(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.swagger.model.DocumentsPaginatedResult], message = "")))
  def getDocumentsByFilters(status: Option[String], documentType: Option[String],
    customerId: Option[UUIDLike],
    customerFullName: Option[String],
    customerMsisdn: Option[String],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate], isCheckedAt: Option[Boolean],
    partialMatch: Option[String],
    ordering: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.DocumentToCreate",
      example = "", //not showing correctly
      paramType = "body",
      name = "DocumentToCreate")))
  def createDocument(): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt"),
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "java.io.File",
      paramType = "form",
      name = "docfile"))) //conf.get[String]("document.multipart-form-data.doc-key")
  def uploadDocumentFile(id: UUID): Action[MultipartFormData[Files.TemporaryFile]]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def approveDocument(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.RejectionReason",
      example = "", //not showing correctly
      paramType = "body",
      name = "RejectionReason")))
  def rejectDocument(id: UUID): Action[String]

}
