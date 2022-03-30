package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.customer.dto.ExternalAccountToRead
import tech.pegb.backoffice.api.swagger.model.ExternalAccountsPaginatedResult
import tech.pegb.backoffice.util.UUIDLike

@Api(value = "ExternalAccounts", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.ExternalAccountsController])
trait ExternalAccountsController extends Routable {

  override def getRoute: String = "external_accounts"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ExternalAccountToRead], message = "")))
  def getExternalAccount(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ExternalAccountsPaginatedResult], message = "")))
  def getExternalAccountsByCriteria(
    customerId: Option[UUIDLike],
    customerName: Option[String],
    currency: Option[String],
    providerName: Option[String],
    accountNumber: Option[String],
    accountHolder: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ExternalAccountToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ExternalAccountToCreate",
      example = "",
      paramType = "body",
      name = "ExternalAccountToCreate")))
  def createExternalAccount: Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ExternalAccountToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ExternalAccountToUpdate",
      example = "",
      paramType = "body",
      name = "ExternalAccountToUpdate")))
  def updateExternalAccount(id: UUID): Action[String]

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
  def deleteExternalAccount(id: UUID): Action[String]

}
