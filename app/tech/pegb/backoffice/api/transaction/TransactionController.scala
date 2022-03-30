package tech.pegb.backoffice.api.transaction

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.swagger.model.TransactionPaginatedResult
import tech.pegb.backoffice.api.transaction.dto.TransactionToRead
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "Transactions", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.TransactionController])
trait TransactionController extends Routable {
  def getRoute: String = "transactions"
  @ApiOperation(value = "Returns list of transactions belonging to a single business transaction")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TransactionPaginatedResult], message = "")))
  def getTransactionById(id: String): Action[AnyContent]

  @ApiOperation(value = "Returns list of transactions")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TransactionPaginatedResult], message = "")))
  def getTransactions(
    anyCustomerName: Option[String],
    customerId: Option[UUIDLike],
    accountId: Option[UUIDLike],
    @ApiParam(name = "date_from", example = "2019-01-01 or 2019-01-01T00:00:00") dateFrom: Option[LocalDateTimeFrom],
    @ApiParam(name = "date_to", example = "2019-01-31 or 2019-01-31T23:59:59") dateTo: Option[LocalDateTimeTo],
    @ApiParam(name = "type") `type`: Option[String],
    channel: Option[String],
    status: Option[String],
    partial_match: Option[String],
    orderBy: Option[String],
    @ApiParam(value = "For pagination, check application config for pagination.max-cap (maximum allowable rows to query) and pagination.defaultLimit (default value for this field)") limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiOperation(value = "Returns list of transactions cancelled")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[TransactionToRead]], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.transaction.dto.TxnToUpdateForCancellation",
      example = "",
      paramType = "body",
      name = "Cancel Transaction")))
  def cancelTransaction(id: String): Action[String]

  @ApiOperation(value = "Returns list of new transactions which reversed the effect of old transactions")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.transaction.dto.TxnToUpdateForReversal",
      example = "",
      paramType = "body",
      name = "Reverse Transaction")))
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[TransactionToRead]], message = "")))
  def revertTransaction(id: String): Action[String]
}
