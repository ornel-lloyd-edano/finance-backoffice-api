package tech.pegb.backoffice.api.currencyexchange

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.currencyexchange.dto.SpreadToRead
import tech.pegb.backoffice.api.swagger.model.SpreadsPaginatedResult
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Spreads", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.SpreadsController])
trait SpreadsController extends Routable {

  override def getRoute: String = "spreads"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadToRead], message = "")))
  def getSpread(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadToRead], message = "")))
  def getCurrencyExchangeSpread(fxId: UUID, id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadsPaginatedResult], message = "")))
  def getSpreadsByCriteria(
    id: Option[UUIDLike],
    currencyExchangeId: Option[UUIDLike],
    currency: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    institution: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.SpreadToCreateWithFxId",
      example = "",
      paramType = "body",
      name = "SpreadToCreate")))
  def createSpreads(): Action[String]

}
