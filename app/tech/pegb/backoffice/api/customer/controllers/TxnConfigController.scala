package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.customer.dto.{TxnConfigToRead}
import tech.pegb.backoffice.api.swagger.model.{TxnConfigPaginatedResult}
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "TransactionConfig", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.TxnConfigController])
trait TxnConfigController extends Routable {

  override def getRoute: String = "txn_config"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TxnConfigToRead], message = "")))
  def getTxnConfig(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TxnConfigPaginatedResult], message = "")))
  def getTxnConfigByCriteria(
    customerId: Option[UUIDLike],
    customerName: Option[String],
    currency: Option[String],
    transactionType: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TxnConfigToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.TxnConfigToCreate",
      example = "",
      paramType = "body",
      name = "TxnConfigToCreate")))
  def createTxnConfig: Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TxnConfigToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.TxnConfigToUpdate",
      example = "",
      paramType = "body",
      name = "TxnConfigToUpdate")))
  def updateTxnConfig(id: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deleteTxnConfig(id: UUID): Action[String]

}
