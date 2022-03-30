package tech.pegb.backoffice.api.transaction

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.swagger.model.{ManualTransactionPaginatedResult, ManualTransactionToRead}
import tech.pegb.backoffice.api.transaction.dto.{SettlementFxHistoryToRead, SettlementRecentAccountToRead}
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Manual Transactions", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.ManualTransactionController])
trait ManualTransactionController extends Routable {
  def getRoute = "manual_transactions"

  @ApiOperation(value = "Returns list of manual transactions")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ManualTransactionPaginatedResult], message = "")))
  def getManualTransactions(
    @ApiParam(name = "id", example = "a37c9454-8067-4dc1-9319-3034ddc9919c") id: Option[UUIDLike],
    @ApiParam(name = "date_from", example = "2019-01-01 or 2019-01-01T00:00:00") dateFrom: Option[LocalDateTimeFrom],
    @ApiParam(name = "date_to", example = "2019-01-31 or 2019-01-31T23:59:59") dateTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    @ApiParam(value = "For pagination, check application config for pagination.max-cap (maximum allowable rows to query) and pagination.defaultLimit (default value for this field)") limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiOperation(value = "Create a manual transaction settlement")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ManualTransactionToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ManualTransactionToCreate",
      example = "",
      paramType = "body",
      name = "ManualTransactionToCreate")))
  def createManualTransaction: Action[String]

  @ApiOperation(value = "Returns list settlement fx history")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[SettlementFxHistoryToRead]], message = "")))
  def getSettlementFxHistory(
    provider: Option[String],
    fromCurrency: Option[String],
    toCurrency: Option[String],
    dateFrom: Option[LocalDateTimeFrom],
    dateTimeTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  @ApiOperation(value = "Returns list of fx settlement recent accounts")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[SettlementRecentAccountToRead]], message = "")))
  def getSettlementRecentAccount(
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]
}
