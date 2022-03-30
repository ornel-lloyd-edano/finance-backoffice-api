package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.customer.dto.{ContactAddressToRead, ContactToRead, VelocityPortalUserToRead}
import tech.pegb.backoffice.api.swagger.model.{ContactAddressToReadPaginatedResult, ContactToReadPaginatedResult, VelocityPortalUserToReadPaginatedResult}

@Api(value = "BusinessUser", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[impl.BusinessUserController])
trait BusinessUserController extends Routable {

  override def getRoute: String = "business_users"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[VelocityPortalUserToReadPaginatedResult], message = "")))
  def getVelocityPortalUsers(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactToReadPaginatedResult], message = "")))
  def getContacts(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactAddressToReadPaginatedResult], message = "")))
  def getAddress(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[VelocityPortalUserToRead], message = "")))
  def getVelocityPortalUserById(
    userId: UUID,
    vpUserId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactToRead], message = "")))
  def getContactsById(
    userId: UUID,
    contactId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactAddressToRead], message = "")))
  def getAddressById(
    userId: UUID,
    addressId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Unit], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.VelocityPortalResetPinRequest",
      example = "", //not showing correctly
      paramType = "body",
      name = "VelocityPortalResetPinRequest")))
  def resetVelocityPortalPin(
    userId: UUID,
    vpUserId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ContactToCreate",
      example = "",
      paramType = "body",
      name = "ContactToCreate")))
  def createContact(userId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ContactToUpdate",
      example = "",
      paramType = "body",
      name = "ContactToUpdate")))
  def updateContact(userId: UUID, contactId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ContactAddressToCreate",
      example = "",
      paramType = "body",
      name = "ContactAddressToCreate")))
  def createAddress(userId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ContactToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.ContactAddressToUpdate",
      example = "",
      paramType = "body",
      name = "ContactAddressToUpdate")))
  def updateAddress(userId: UUID, addressId: UUID): Action[String]

}
