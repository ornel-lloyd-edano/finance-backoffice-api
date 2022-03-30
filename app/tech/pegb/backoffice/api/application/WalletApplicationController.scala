package tech.pegb.backoffice.api.application

import java.time.LocalDate
import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations.{Api, _}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.application.dto.{WalletApplicationDetail, WalletApplicationToRead}
import tech.pegb.backoffice.api.swagger.model.{ApplicationPaginatedResult, DocumentsPaginatedResult}

@Api(value = "Wallet Applications", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.WalletApplicationController])
trait WalletApplicationController extends Routable {

  def getRoute: String = "wallet_applications"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[WalletApplicationDetail], message = "")))
  def getWalletApplication(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ApplicationPaginatedResult], message = "")))
  def getWalletApplicationsByCriteria(
    status: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    msisdn: Option[String],
    nationalId: Option[String],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    orderBy: Option[String],
    limit: Option[Int] = None,
    offset: Option[Int] = None): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[DocumentsPaginatedResult], message = "")))
  def getDocumentsByWalletApplication(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[WalletApplicationToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def approveWalletApplication(id: UUID): Action[String]

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "Reject Wallet Application",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.WalletApplicationToReject",
      paramType = "body",
      name = "Reject wallet application...")))
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[WalletApplicationToRead], message = "")))
  def rejectWalletApplication(id: UUID): Action[JsValue]

}
