package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.customer.dto.{AccountToRead, GenericUserToRead, PaymentOptionToRead}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.swagger.model.{AccountPaginatedResult, GenericUserPaginatedResult, TransactionPaginatedResult}
import tech.pegb.backoffice.util.UUIDLike

@ImplementedBy(classOf[impl.CustomersController])
@Api(value = "Customers", produces = "application/json", consumes = "application/json")
trait CustomersController extends Routable {
  def getRoute: String = "customers"
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountPaginatedResult], message = "")))
  def getCustomerAccounts(
    id: UUID,
    primaryAccount: Option[Boolean],
    accountType: Option[String],
    accountNumber: Option[String],
    status: Option[String],
    currency: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def getCustomerAccountById(
    id: UUID,
    accountId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def activateCustomerAccount(customerId: UUID, accountId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def deactivateCustomerAccount(customerId: UUID, accountId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def closeCustomerAccount(customerId: UUID, accountId: UUID): Action[String]

  @ApiOperation(value = "Returns list of transactions related to accountId")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TransactionPaginatedResult], message = "")))
  def getTransactions(
    customerId: UUID,
    accountId: UUID,
    @ApiParam(name = "date_from", example = "2019-01-01 or 2019-01-01T00:00:00") dateFrom: Option[LocalDateTimeFrom],
    @ApiParam(name = "date_to", example = "2019-01-31 or 2019-01-31T23:59:59") dateTo: Option[LocalDateTimeTo],
    @ApiParam(name = "type") `type`: Option[String],
    channel: Option[String],
    status: Option[String],
    orderBy: Option[String],
    @ApiParam(value = "For pagination, check application config for pagination.max-cap (maximum allowable rows to query) and pagination.defaultLimit (default value for this field)") limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiOperation(value = "Returns list of payment options for customer")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[PaymentOptionToRead]], message = "")))
  def getPaymentOptionsTransactions(customerId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[GenericUserPaginatedResult], message = "")))
  def getUserByCriteria(
    msisdn: Option[String],
    userId: Option[UUIDLike],
    alias: Option[String],
    fullname: Option[String],
    status: Option[String],
    anyName: Option[String],
    @ApiParam(required = false, defaultValue = "user_id, msisdn, full_name, any_name", allowableValues = "disabled, user_id, msisdn, full_name, any_name", allowMultiple = false) partialMatch: Option[String],
    @ApiParam(value = "order by results by certain field or multiple fields", required = false, defaultValue = "",
      allowableValues = "company,created_at,created_by,document_number,document_type,email,employer,fullname,gender,msisdn,alias,nationality,occupation,person_id,segment,status,subscription,tier,type,updated_at,updated_by,user,username,-company,-created_at,-created_by,-document_number,-document_type,-email,-employer,-fullname,-gender,-msisdn,-name,-nationality,-occupation,-person_id,-segment,-status,-subscription,-tier,-type,-updated_at,-updated_by,-user,-username",
      allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[GenericUserToRead], message = "")))
  def getUser(id: UUID): Action[AnyContent]

}
