package tech.pegb.backoffice.api.customer.controllers.impl

import java.time.{LocalDate, LocalTime, ZoneId}
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.customer.dto.AccountToCreate
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model._
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.account.model.AccountAttributes.AccountNumber
import tech.pegb.backoffice.domain.customer.abstraction.CustomerAccount
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.settings.abstraction.SystemSettingService
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    customerAccount: CustomerAccount,
    accountService: AccountManagement,
    systemSettingService: SystemSettingService,
    txnService: TransactionManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with controllers.AccountController {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  val AccountValidSorter = Set("number", "name", "account_type_name", "is_main_account", "currency_name", "balance",
    "blocked_balance", "status", "closed_at", "last_transaction_at", "created_at", "created_by", "updated_at", "updated_by")

  def createAccount(): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.toString.as(classOf[AccountToCreate], isDeserializationStrict)
        .toEither.leftMap(_.log()
          .asMalformedRequestApiError("Malformed request to create account. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      dto ← EitherT.fromEither[Future](parsedRequest.asDomain(doneBy, doneAt)
        .toEither.leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to create account. Value of a field is empty, not in the correct format or not among the expected values.".toOption)))

      result ← EitherT(accountService.createAccount(dto).map(_.map(_.asApi.toJsonStr))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def getAccountById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    accountService.getAccountById(id).map(result ⇒
      handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getAccountByAccountNumber(accountNumber: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    customerAccount.getAccountByAccountNumber(AccountNumber(accountNumber.sanitize))
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getAccountByAccountName(accountName: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    //TODO remove NameAttribute here
    customerAccount.getAccountByAccountName(NameAttribute(accountName.sanitize))
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())))
  }

  def activateCustomerAccount(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT.fromEither[Future] {
        if (ctx.hasBody)
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to activate account".toOption))
        else Right(GenericRequestWithUpdatedAt.empty)
      }
      result ← EitherT(accountService.activateAccount(id, doneBy, doneAt, parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield result.asApi.toJsonStr).value.map(handleApiResponse(_))
  }

  def closeCustomerAccount(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT.fromEither[Future] {
        if (ctx.hasBody)
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to activate account".toOption))
        else Right(GenericRequestWithUpdatedAt.empty)
      }
      result ← EitherT(accountService.deleteAccount(id, doneBy, doneAt, parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield result.asApi.toJsonStr).value.map(handleApiResponse(_))

  }

  def deactivateCustomerAccount(id: UUID): Action[String] = LoggedAsyncAction(freeText) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneBy = getRequestFrom
    val doneAt = getRequestDate.toLocalDateTimeUTC

    (for {
      parsedRequest ← EitherT.fromEither[Future] {
        if (ctx.hasBody)
          ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
            .toEither.leftMap(_.log().asMalformedRequestApiError("Malformed request to activate account".toOption))
        else Right(GenericRequestWithUpdatedAt.empty)
      }
      result ← EitherT(accountService.blockAccount(id, doneBy, doneAt, parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield result.asApi.toJsonStr).value.map(handleApiResponse(_))
  }

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
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {

      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(AccountValidSorter).leftMap(_.log().asInvalidRequestApiError("Invalid field found in order_by of accounts.".toOption)))

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(Constants.validAccountsPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((customerId, customerFullName, anyCustomerName, msisdn, isMainAccount,
        currency, status, accountType, accountNumber, partialMatchFields).asDomain.toEither
        .leftMap(_.log().asInvalidRequestApiError(("Invalid request to fetch accounts." +
          " A query parameter might have an empty value, wrong format or not among the possible values.").toOption)))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of accounts".asUnknownApiError)))

      total ← EitherT(executeIfGET(accountService.countAccountsByCriteria(criteria)
        .map(_.leftMap(_ ⇒ "Failed counting accounts".asUnknownApiError))
        .futureWithTimeout.recover { case e: Throwable ⇒ Right(-1) }, NoCount.toFuture))

      accounts ← EitherT(executeIfGET(accountService.getAccountsByCriteria(criteria, ordering, limit, offset)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      (PaginatedResult(total, accounts.map(_.asApi), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getFloatAccountAggregations(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    //TODO remove this once front end fix the mapping issue with json
    val limit = None
    val offset = None

    (for {

      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(Set("updated_at", "created_at")).leftMap(_.log().asInvalidRequestApiError("Invalid field found in order_by of accounts.".toOption)))

      dateRangeQueryParam ← EitherT.fromEither[Future]((dateFrom, dateTo) match {
        case (Some(from), Some(to)) if from.localDateTime.isAfter(to.localDateTime) ⇒
          "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft

        case _ ⇒
          val defaultDateFrom = LocalDate.now(ZoneId.of("UTC")).atStartOfDay()
          val defaultDateTo = LocalDate.now(ZoneId.of("UTC")).atEndOfDay

          val from = dateFrom.map(_.localDateTime.`with`(LocalTime.MIN)).getOrElse(defaultDateFrom)
          val to = dateTo.map(_.localDateTime.`with`(LocalTime.MAX)).getOrElse(defaultDateTo)
          (from, to).toRight
      })

      accountNumbers ← EitherT(systemSettingService
        .getSystemSettingArrayValueByKey(appConfig.FloatAccountNumbersKey)).leftMap(_.asApiError())

      txnCriteria ← EitherT.fromEither[Future] {

        if (accountNumbers.nonEmpty) {

          (accountNumbers, dateRangeQueryParam._1, dateRangeQueryParam._2).asDomain.toRight
        } else {

          s"""there are no account numbers set up in system setting, key: ${appConfig.FloatAccountNumbersKey}""".asNotFoundApiError.toLeft
        }

      }

      latestVersion ← EitherT(latestVersionService.getLatestVersion(txnCriteria).map(_.leftMap(_.asApiError())))

      result ← EitherT(executeIfGET(accountService.executeOnFlyAggregation(txnCriteria, ordering, limit, offset)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      //(PaginatedResult(txnCriteria.accountNumbers.size, result.map(_.asApi), limit, offset).toJsonStr, latestVersion)
      (result.map(_.asApi).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }
}

