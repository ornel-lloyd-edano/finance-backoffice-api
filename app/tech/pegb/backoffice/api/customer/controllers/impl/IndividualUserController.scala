package tech.pegb.backoffice.api.customer.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.application.dto.WalletApplicationToReject
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.customer.dto.{AccountToCreate, CustomerAccountToCreate, IndividualUserToUpdate}
import tech.pegb.backoffice.api.document.dto.RejectionReason
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.domain.customer.abstraction.{CustomerAccount, CustomerActivation, CustomerRead, CustomerUpdate, _}
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.api.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.api.domain.document.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.application.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.document.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualUserController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    customerRead: CustomerRead,
    customerActivation: CustomerActivation,
    customerAccount: CustomerAccount,
    customerUpdate: CustomerUpdate,
    customerWalletApplication: CustomerWalletApplication,
    documentManagement: DocumentManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with controllers.IndividualUserController {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  val individualUserValidSorter = Set("user", "username", "tier", "segment", "subscription", "email", "status",
    "msisdn", "type", "name", "fullname", "gender", "person_id", "document_number", "document_type", "nationality",
    "occupation", "company", "employer", "created_at", "created_by", "updated_at", "updated_by")

  def getIndividualUser(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    customerRead.getIndividualUser(id).map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr)
      .leftMap(_.asApiError())))
  }

  def activateIndividualUser(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    customerActivation.activateIndividualUser(customerId, doneBy, doneAt)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def deactivateIndividualUser(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    customerActivation.deactivateIndividualUser(customerId, doneBy, doneAt)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def openIndividualUserAccount(userId: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.toString
        .as(classOf[CustomerAccountToCreate], isDeserializationStrict).toEither
        .leftMap(_.log().asMalformedRequestApiError("Malformed request to create individual_user account. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      dto ← EitherT.fromEither[Future](AccountToCreate(
        customerId = userId,
        `type` = parsedRequest.`type`,
        currency = parsedRequest.currency).asDomain(doneBy, doneAt).toEither
        .leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to create individual_user account. Value of a field is empty, not in the correct format or not among the expected values.".toOption)))

      result ← EitherT(customerAccount.openIndividualUserAccount(userId, dto).map(_.map(_.asApi.toJsonStr))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateIndividualUser(customerId: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.toString
        .as(classOf[IndividualUserToUpdate], isDeserializationStrict)
        .toEither.leftMap(_.log()
          .asMalformedRequestApiError("Malformed request to update individual_user. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      dto ← EitherT.fromEither[Future](parsedRequest.asDomain.toEither
        .leftMap(_.log().asInvalidRequestApiError("Invalid request to update individual_user. Value of a field is empty, not in the correct format or not among the expected values.".toOption)))

      result ← EitherT(customerUpdate.updateIndividualUser(customerId, dto, doneBy, doneAt)
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def getIndividualUserAccounts(
    customerId: UUID,
    primaryAccount: Option[Boolean],
    accountType: Option[String],
    accountNumber: Option[String],
    status: Option[String],
    currency: Option[String]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    val criteria = (customerId, primaryAccount, currency, status, accountType, accountNumber).asDomain

    (for {
      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of individual_user accounts".asUnknownApiError)))

      accounts ← EitherT(executeIfGET(customerAccount.getAccountsByCriteria(
        criteria = criteria, orderBy = Nil, limit = None, offset = None)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      val result = PaginatedResult(
        total = accounts.size,
        results = accounts.map(_.asApi), None, None).toJsonStr

      (result, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getIndividualUserAccount(customerId: UUID, accntId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    customerAccount.getAccountsByCriteria(
      criteria = customerId.toAccountCriteria,
      orderBy = Nil,
      limit = None,
      offset = None).map(_.fold(
      _.asApiError().toLeft,
      _.find(_.id == accntId) match {
        case None ⇒ s"Account with id $accntId of user $customerId was not found.".asNotFoundApiError.toLeft
        case Some(account) ⇒ account.asApi.toJsonStr.toRight
      })).map(handleApiResponse(_))
  }

  def activateIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    customerAccount.activateIndividualUserAccount(customerId, accountId, doneBy, doneAt)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def deactivateIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    customerAccount.deactivateIndividualUserAccount(customerId, accountId, doneBy, doneAt)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def closeIndividualUserAccount(customerId: UUID, accountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    customerAccount.closeIndividualUserAccount(customerId, accountId, doneBy, doneAt)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getIndividualUserWalletApplications(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    (for {
      walletApplication ← EitherT(customerWalletApplication.getWalletApplicationsByUserId(customerId)
        .map(_.leftMap(_.asApiError())))

    } yield PaginatedResult(walletApplication.size, walletApplication.map(_.asApi), None, None).toJsonStr)
      .value.map(handleApiResponse(_))
  }

  def getIndividualUserWalletApplicationByApplicationId(customerId: UUID, applicationId: UUID): Action[AnyContent] =
    LoggedAsyncAction { implicit ctx ⇒
      implicit val requestId = getRequestId
      (for {
        walletApplication ← EitherT(customerWalletApplication
          .getWalletApplicationByApplicationIdAndUserId(customerId, applicationId)
          .map(_.leftMap(_.asApiError())))

      } yield walletApplication.asApi.toJsonStr)
        .value.map(handleApiResponse(_))
    }

  def approveWalletApplicationByUserId(customerId: UUID, applicationId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate

    (for {
      requestWithUpdatedAt ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.asApiError()))

      result ← EitherT(customerWalletApplication.approveWalletApplicationByUserId(
        customerId,
        applicationId,
        doneBy,
        doneAt.toLocalDateTimeUTC,
        requestWithUpdatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))

  }

  def rejectWalletApplicationByUserId(customerId: UUID, applicationId: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate

    (for {
      requestWithUpdatedAt ← EitherT.fromEither[Future](ctx.body.toString().as(classOf[WalletApplicationToReject], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to reject wallet application. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      result ← EitherT(customerWalletApplication.rejectWalletApplicationByUserId(
        customerId,
        applicationId,
        doneBy,
        doneAt.toLocalDateTimeUTC,
        requestWithUpdatedAt.reason,
        requestWithUpdatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def getIndividualUsersByCriteria(
    msisdn: Option[String],
    userId: Option[UUIDLike],
    name: Option[String],
    fullName: Option[String],
    status: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId = getRequestId

    (for {

      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(individualUserValidSorter).leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to fetch individual users. Value of order_by is not among the expected values.".toOption)))

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(Constants.validIndividualUsersPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((msisdn, userId, name, fullName, status, partialMatchFields).asDomain
        .toEither.leftMap(_.log().asInvalidRequestApiError("Invalid request to fetch individual users. Value of a query parameter is not in the correct format or not among the expected values.".toOption)))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of individual users".asUnknownApiError)))

      total ← EitherT(executeIfGET(customerRead.countIndividualUsersByCriteria(criteria)
        .map(_.leftMap(_.asApiError())), NoCount.toFuture).futureWithTimeout
        .recover {
          case e: Throwable ⇒ Right(-1)
        })

      results ← EitherT(executeIfGET(customerRead.findIndividualUsersByCriteria(
        criteria, ordering, limit, offset).map(_.leftMap(_ ⇒ "Failed fetching individual users".asUnknownApiError)), NoResult.toFuture))

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getIndividualUsersDocuments(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    val criteria = customerId.asDocumentCriteria

    (for {
      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed to get latest version of individual user's documents".asInvalidRequestApiError)))

      total ← EitherT(executeIfGET(documentManagement.countDocumentsByCriteria(criteria)
        .map(_.leftMap(_ ⇒ "Failed to count individual user documents".asUnknownApiError)), NoCount.toFuture))

      results ← EitherT(executeIfGET(documentManagement.getDocumentsByCriteria(criteria, Nil, None, None)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))
    } yield {
      (PaginatedResult(total = total, results = results.map(_.asApi), None, None).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getIndividualUsersDocumentByDocId(customerId: UUID, docId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    validateDocumentOwner(customerId, docId).map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr)))
  }

  def rejectDocument(customerId: UUID, docId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[RejectionReason], isDeserializationStrict)
        .toEither.leftMap(_.log()
          .asMalformedRequestApiError("Malformed request to reject individual_user's document. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      _ ← EitherT(validateDocumentOwner(customerId, docId))

      rejectResult ← {
        val dto = (docId, getRequestFrom, getRequestDate, parsedRequest.reason, parsedRequest.lastUpdatedAt).asDomain
        EitherT(documentManagement.rejectDocument(dto).map(_.leftMap(_.asApiError())))
      }

    } yield {
      rejectResult.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def approveDocument(customerId: UUID, docId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log()
          .asMalformedRequestApiError("Malformed request to approve individual_user's document. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      _ ← EitherT(validateDocumentOwner(customerId, docId))

      rejectResult ← {
        val dto = (docId, doneBy, doneAt, parsedRequest.lastUpdatedAt).asDomain
        EitherT(documentManagement.approveDocument(dto).map(_.leftMap(_.asApiError())))
      }

    } yield {
      rejectResult.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  //TODO consider moving to domain later
  private def validateDocumentOwner(customerId: UUID, docId: UUID)(implicit requestId: UUID): Future[Either[ApiError, Document]] = {
    val getCustomerResult = customerRead.getIndividualUser(customerId)
    val getDocumentResult = documentManagement.getDocument(docId)
    (for {
      customer ← EitherT(getCustomerResult)
      document ← EitherT(getDocumentResult)
    } yield {
      document
    }).value.map {
      case Right(document) if document.customerId.contains(customerId) ⇒ Right(document)
      case Right(document) ⇒ Left(ApiError(requestId, ApiErrorCodes.InvalidRequest, s"Document id ${document.id} does not belong to User $customerId"))
      case Left(error) ⇒ Left(error.asApiError())
    }
  }
}
