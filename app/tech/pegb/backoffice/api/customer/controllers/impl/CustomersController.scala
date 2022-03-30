package tech.pegb.backoffice.api.customer.controllers.impl

import java.time.ZonedDateTime
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api.transaction.{Constants ⇒ TxnConstants}
import tech.pegb.backoffice.domain.customer.abstraction._
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future
//@Api("Customers")
@Singleton
class CustomersController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    customerRead: CustomerRead,
    customerUpdate: CustomerUpdate,
    customerRegistration: CustomerRegistration,
    customerAccount: CustomerAccount,
    transactionManagement: TransactionManagement,
    paymentOptionService: PaymentOptionDomain,
    latestVersionService: LatestVersionService,
    accountController: AccountController,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with controllers.CustomersController {

  import ApiController._
  import ApiErrors._
  import CustomersController._
  import RequiredHeaders._

  implicit val executionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout = appConfig.FutureTimeout
  val PaginationMaxCap = appConfig.PaginationMaxLimit

  override def getCustomerAccounts(
    id: UUID,
    primaryAccount: Option[Boolean],
    accountType: Option[String],
    accountNumber: Option[String],
    status: Option[String],
    currency: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = {

    accountController.getAccountsByCriteria(
      customerId = UUIDLike(id.toString).some,
      customerFullName = None,
      anyCustomerName = None,
      msisdn = None,
      isMainAccount = primaryAccount,
      currency = currency,
      status = status,
      accountType = accountType,
      accountNumber = accountNumber,
      partialMatch = partialMatch,
      orderBy = orderBy,
      limit = limit,
      offset = offset)
  }

  def getCustomerAccountById(id: UUID, accountId: UUID): Action[AnyContent] = {
    accountController.getAccountById(accountId)
  }

  def activateCustomerAccount(customerId: UUID, accountId: UUID): Action[String] = {
    accountController.activateCustomerAccount(accountId)
  }

  def deactivateCustomerAccount(customerId: UUID, accountId: UUID): Action[String] = {
    accountController.deactivateCustomerAccount(accountId)
  }

  def closeCustomerAccount(customerId: UUID, accountId: UUID): Action[String] = {
    accountController.closeCustomerAccount(accountId)
  }

  def getTransactions(
    customerId: UUID,
    accountId: UUID,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    `type`: Option[String],
    channel: Option[String],
    status: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      dateRangeQueryParam ← EitherT.fromEither[Future]((dateFrom, dateTo) match {
        case (Some(from), Some(to)) if from.localDateTime.isAfter(to.localDateTime) ⇒
          "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft

        case _ ⇒
          (dateFrom, dateTo).toRight
      })

      validLimit ← EitherT.fromEither[Future](limit match {
        case Some(limit) if limit > PaginationMaxCap ⇒
          Left(s"Limit provided(${limit}) is greater than PaginationMaxCap ${PaginationMaxCap}".asInvalidRequestApiError)
        case _ ⇒ Right(limit)
      })

      criteria ← EitherT.fromEither[Future]((None, None, customerId.toUUIDLike.toOption, accountId.toUUIDLike.toOption, dateFrom, dateTo, `type`, channel, status, None, Set[String]()).asDomain
        .toEither.leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to customer transactions. A query parameter might have an empty value, wrong format or not among the possible values.".toOption)))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of customer transactions".asUnknownApiError)))

      total ← EitherT(executeIfGET(transactionManagement.countTransactionsByCriteria(criteria)
        .map(_.leftMap(_.asApiError())), NoCount.toFuture))

      ordering ← EitherT.fromEither[Future](
        orderBy.orElse(Some(TxnConstants.defaultOrdering)).validateOrdering(TxnConstants.validOrderByFields).leftMap(_.log().asInvalidRequestApiError()))

      transactions ← EitherT(executeIfGET(transactionManagement.getTransactionsByCriteria(criteria, ordering, validLimit, offset)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      (PaginatedResult(total, transactions.map(_.asApi()), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  override def getPaymentOptionsTransactions(customerId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    paymentOptionService.getPaymentOptions(customerId).map(result ⇒
      handleApiResponse(result.map(_.toJsonStr).leftMap(_.asApiError())))
  }

  def getUserByCriteria(
    msisdn: Option[String],
    userId: Option[UUIDLike],
    alias: Option[String],
    fullname: Option[String],
    status: Option[String],
    anyName: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      partialMatchSet ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(userPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.map(_.replace("customer_type", "type")).validateOrdering(userValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (msisdn, userId, alias, fullname, status, anyName, partialMatchSet).asDomain.toEither
          .leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(customerRead.countUserByCriteria(criteria).futureWithTimeout, NoCount.toFuture))
        .leftMap(_.asApiError("failed to get the count of users".some))

      users ← EitherT(executeIfGET(
        customerRead.getUserByCriteria(criteria, ordering, limit, offset).futureWithTimeout, NoResult.toFuture))
        .leftMap(_.asApiError("failed to get the user list".some))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria))
        .leftMap(_.asApiError("failed to get the latest version".some))

    } yield (PaginatedResult(count, users.map(_.asApi), limit, offset).toJsonStrWithoutEscape, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
      }
  }

  def getUser(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    customerRead.getUser(id).map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr)
      .leftMap(_.asApiError())))
  }

  def createBusinessUser() = ???

  def getBusinessUserById(id: UUID) = ???

  def getBusinessUsers(`type`: Option[String], company: Option[String], username: Option[String], tier: Option[String], subscription: Option[String], segment: Option[String], status: Option[String], createdBy: Option[UUID], updatedBy: Option[UUID], createdDateFrom: Option[ZonedDateTime], createdDateTo: Option[ZonedDateTime], updatedDateFrom: Option[ZonedDateTime], updatedDateTo: Option[ZonedDateTime], maybeLimit: Option[Int], maybeOffset: Option[Int]) = ???

  def updateBusinessUser(id: UUID) = ???

  def activateBusinessUser(id: UUID) = ???

  def deactivateBusinessUser(id: UUID) = ???

  def deleteBusinessUser(id: UUID) = ???
}

object CustomersController {
  val userValidSorter = Set("username", "tier", "segment", "subscription", "email", "status",
    "msisdn", "type", "name", "fullname", "gender", "person_id", "document_number", "document_type", "nationality",
    "occupation", "company", "employer", "created_at", "created_by", "updated_at", "updated_by")

  val userPartialMatchFields = Set("disabled", "user_id", "msisdn", "full_name", "any_name")

}
