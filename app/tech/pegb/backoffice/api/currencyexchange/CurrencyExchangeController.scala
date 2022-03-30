package tech.pegb.backoffice.api.currencyexchange

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.currencyexchange.dto.{CurrencyExchangeToRead, CurrencyExchangeToReadDetails, SpreadToRead}
import tech.pegb.backoffice.api.swagger.model.{CurrencyExchangePaginatedResult, SpreadsPaginatedResult}
import tech.pegb.backoffice.util.UUIDLike

@ImplementedBy(classOf[controllers.CurrencyExchangeController])
@Api(value = "Currency Exchange", produces = "application/json", consumes = "application/json")
trait CurrencyExchangeController extends Routable {

  override def getRoute: String = "currency_exchanges"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyExchangePaginatedResult], message = "")))
  def getCurrencyExchangeByCriteria(
    id: Option[UUIDLike],
    currencyCode: Option[String],
    baseCurrency: Option[String],
    provider: Option[String],
    status: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    @ApiParam(value = "For pagination, check application config for pagination.max-cap (maximum allowable rows to query) and pagination.defaultLimit (default value for this field)") limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyExchangeToReadDetails], message = "")))
  def getCurrencyExchange(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadsPaginatedResult], message = "")))
  def getCurrencyExchangeSpreads(
    currencyExchangeId: UUID,
    transactionType: Option[String],
    channel: Option[String],
    institution: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyExchangeToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def activateFX(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyExchangeToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deactivateFX(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.SpreadToCreate",
      example = "",
      paramType = "body",
      name = "SpreadToCreate")))
  def createSpreads(currencyExchangeId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.SpreadToUpdate",
      example = "",
      paramType = "body",
      name = "SpreadToUpdate")))
  def updateCurrencyExchangeSpread(spreadId: UUID, fxId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SpreadToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteCurrencyExchangeSpread(spreadId: UUID, fxId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  def batchActivateFX: Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  def batchDeactivateFX: Action[AnyContent]
}
