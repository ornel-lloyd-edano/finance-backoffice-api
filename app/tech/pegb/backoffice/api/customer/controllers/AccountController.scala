package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.customer.dto.{AccountToRead, FloatAccountAggregationToRead}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.swagger.model.AccountPaginatedResult
import tech.pegb.backoffice.util.UUIDLike

@ImplementedBy(classOf[impl.AccountController])
@Api(value = "Accounts", produces = "application/json", consumes = "application/json")
trait AccountController extends Routable {
  def getRoute: String = "accounts"
  @ApiOperation(value = "", hidden = true)
  def createAccount(): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def getAccountById(id: UUID): Action[AnyContent]

  @ApiOperation(value = "", hidden = true)
  def getAccountByAccountNumber(accountNumber: String): Action[AnyContent]

  @ApiOperation(value = "", hidden = true)
  def getAccountByAccountName(accountName: String): Action[AnyContent]

  @ApiOperation(value = "", hidden = true)
  def activateCustomerAccount(id: UUID): Action[String]

  @ApiOperation(value = "", hidden = true)
  def closeCustomerAccount(id: UUID): Action[String]

  @ApiOperation(value = "", hidden = true)
  def deactivateCustomerAccount(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountPaginatedResult], message = "")))
  def getAccountsByCriteria(
    customerId: Option[UUIDLike],
    customerFullName: Option[String],
    anyCustomerName: Option[String],
    msisdn: Option[String],
    isMainAccount: Option[Boolean],
    currency: Option[String],
    status: Option[String],
    accountType: Option[String],
    accountNumber: Option[String],
    @ApiParam(
      required = false,
      defaultValue = "customer_id, customer_full_name, any_customer_name, account_number, msisdn",
      allowableValues = "disabled, customer_id, customer_full_name, any_customer_name, account_number, msisdn", allowMultiple = false) partialMatch: Option[String],
    @ApiParam(value = "order by results by certain field or multiple fields", required = false, defaultValue = "",
      allowableValues = "balance,blocked_balance,closed_at,created_at,created_by,currency,is_main_account,last_transaction_at,name,number,status,type,updated_at,updated_by,-balance,-blocked_balance,-closed_at,-created_at,-created_by,-currency,-is_main_account,-last_transaction_at,-name,-number,-status,-type,-updated_at,-updated_by",
      allowMultiple = false) orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[FloatAccountAggregationToRead]], message = "")))
  def getFloatAccountAggregations(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    @ApiParam(
      value = "order by results by certain field",
      required = false,
      defaultValue = "",
      allowableValues = "created_at, -created_at, updated_at, -updated_at",
      allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

}
