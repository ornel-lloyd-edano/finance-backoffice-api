package tech.pegb.backoffice.api.fee

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.swagger.model.{FeeProfilePaginatedResult, FeeProfileToReadDetails}
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Fee Profiles", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.FeeProfileController])
trait FeeProfileController extends Routable {

  def getRoute: String = "fee_profiles"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[FeeProfileToReadDetails], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.FeeProfileToCreate",
      example = "",
      paramType = "body",
      name = "FeeProfileToCreate")))
  def createFeeProfile: Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[FeeProfileToReadDetails], message = "")))
  def getFeeProfile(id: UUID): Action[AnyContent]

  @ApiOperation(value = "Returns list of fee profile")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[FeeProfilePaginatedResult], message = "")))
  def getFeeProfileByCriteria(
    id: Option[UUIDLike] = None,
    feeType: Option[String] = None,
    userType: Option[String] = None,
    tier: Option[String] = None,
    subscriptionType: Option[String] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    otherParty: Option[String] = None,
    instrument: Option[String] = None,
    calculationMethod: Option[String] = None,
    currencyCode: Option[String] = None,
    feeMethod: Option[String] = None,
    taxIncluded: Option[String] = None,
    partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  //updateFeeProfile behaves also like deleteFeeProfileRange if you update the range field,
  //it will delete the current related fee profile ranges and replace with new ones

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[FeeProfileToReadDetails], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.FeeProfileToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "FeeProfileToUpdate")))
  def updateFeeProfile(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[FeeProfileToReadDetails], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteFeeProfile(id: UUID): Action[String]

}
