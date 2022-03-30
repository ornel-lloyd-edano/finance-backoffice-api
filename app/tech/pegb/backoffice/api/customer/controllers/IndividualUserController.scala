package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.application.dto.WalletApplicationToRead
import tech.pegb.backoffice.api.customer.dto.{AccountToRead, IndividualUserResponse}
import tech.pegb.backoffice.api.swagger.model.{AccountPaginatedResult, ApplicationPaginatedResult, IndividualUsersPaginatedResult}
import tech.pegb.backoffice.util.UUIDLike

@ImplementedBy(classOf[impl.IndividualUserController])
@Api(value = "Individual Users", produces = "application/json", consumes = "application/json")
trait IndividualUserController extends Routable {
  def getRoute: String = "individual_users"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[IndividualUserResponse], message = "")))
  def getIndividualUser(id: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[IndividualUserResponse], message = "")))
  def activateIndividualUser(customerId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[IndividualUserResponse], message = "")))
  def deactivateIndividualUser(customerId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[IndividualUserResponse], message = "")))
  def updateIndividualUser(customerId: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.CustomerAccountToCreate",
      example = "", //not showing correctly
      paramType = "body",
      name = "AccountToCreate")))
  def openIndividualUserAccount(customerId: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountPaginatedResult], message = "")))
  def getIndividualUserAccounts(
    customerId: UUID,
    primaryAccount: Option[Boolean],
    accountType: Option[String],
    accountNumber: Option[String],
    status: Option[String],
    currency: Option[String]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def getIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def activateIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def deactivateIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AccountToRead], message = "")))
  def closeIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[ApplicationPaginatedResult], message = "")))
  def getIndividualUserWalletApplications(customerId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[WalletApplicationToRead], message = "")))
  def getIndividualUserWalletApplicationByApplicationId(customerId: UUID, applicationId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[WalletApplicationToRead], message = "")))
  def approveWalletApplicationByUserId(customerId: UUID, applicationId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[WalletApplicationToRead], message = "")))
  def rejectWalletApplicationByUserId(customerId: UUID, applicationId: UUID): Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[IndividualUsersPaginatedResult], message = "")))
  def getIndividualUsersByCriteria(
    msisdn: Option[String],
    userId: Option[UUIDLike],
    alias: Option[String],
    fullname: Option[String],
    status: Option[String],
    @ApiParam(required = false, defaultValue = "user_id, msisdn, full_name", allowableValues = "disabled, user_id, msisdn, full_name", allowMultiple = false) partialMatch: Option[String],
    @ApiParam(value = "order by results by certain field or multiple fields", required = false, defaultValue = "",
      allowableValues = "company,created_at,created_by,document_number,document_type,email,employer,fullname,gender,msisdn,alias,nationality,occupation,person_id,segment,status,subscription,tier,type,updated_at,updated_by,user,username,-company,-created_at,-created_by,-document_number,-document_type,-email,-employer,-fullname,-gender,-msisdn,-name,-nationality,-occupation,-person_id,-segment,-status,-subscription,-tier,-type,-updated_at,-updated_by,-user,-username",
      allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.swagger.model.DocumentsPaginatedResult], message = "")))
  def getIndividualUsersDocuments(customerId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  def getIndividualUsersDocumentByDocId(customerId: UUID, docId: UUID): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.RejectionReason",
      example = "", //not showing correctly
      paramType = "body",
      name = "RejectionReason")))
  def rejectDocument(customerId: UUID, docId: UUID): Action[String]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[tech.pegb.backoffice.api.document.dto.DocumentToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def approveDocument(customerId: UUID, docId: UUID): Action[String]
}
