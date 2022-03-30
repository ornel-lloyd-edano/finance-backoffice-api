package tech.pegb.backoffice.api.commission.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.commission.dto.{CommissionProfileToRead, CommissionProfileToReadDetails}
import tech.pegb.backoffice.api.swagger.model.CommissionProfilePaginatedResult
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Commission Profiles", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.CommissionProfileController])
trait CommissionProfileController extends Routable {

  def getRoute: String = "commission_profiles"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CommissionProfileToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.CommissionProfileToCreate",
      example = "",
      paramType = "body",
      name = "CommissionProfileToCreate")))
  def createCommissionProfile: Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CommissionProfileToReadDetails], message = "")))
  def getCommissionProfile(id: UUID): Action[AnyContent]

  @ApiOperation(value = "Returns list of commission profile")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CommissionProfilePaginatedResult], message = "")))
  def getCommissionProfileByCriteria(
    id: Option[UUIDLike],
    businessType: Option[String],
    tier: Option[String],
    subscriptionType: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    instrument: Option[String],
    currency: Option[String],
    calculationMethod: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CommissionProfileToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.CommissionProfileToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "CommissionProfileToUpdate")))
  def updateCommissionProfile(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CommissionProfileToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteCommissionProfile(id: UUID): Action[String]
}
