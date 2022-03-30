package tech.pegb.backoffice.api.limit

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.limit.dto.LimitProfileToReadDetail
import tech.pegb.backoffice.api.swagger.model.LimitProfilePaginatedResult
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Limit Profiles", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.LimitProfileController])
trait LimitProfileController extends Routable {
  def getRoute: String = "limit_profiles"
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LimitProfileToReadDetail], message = "")))
  def getLimitProfile(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LimitProfilePaginatedResult], message = "")))
  def getLimitProfileByCriteria(
    id: Option[UUIDLike],
    limitType: Option[String],
    userType: Option[String],
    tier: Option[String],
    subscription: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    otherParty: Option[String],
    instrument: Option[String],
    interval: Option[String],
    currencyCode: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[LimitProfileToReadDetail], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.LimitProfileToCreate",
      example = "",
      paramType = "body",
      name = "LimitProfileToCreate")))
  def createLimitProfile: Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 201, response = classOf[LimitProfileToReadDetail], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.LimitProfileToUpdate",
      example = "",
      paramType = "body",
      name = "LimitProfileToCreate")))
  def updateLimitProfile(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LimitProfileToReadDetail], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteLimitProfile(id: UUID): Action[String]
}
