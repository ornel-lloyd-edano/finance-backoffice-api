package tech.pegb.backoffice.api.transaction.controllers

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api.transaction.Constants
import tech.pegb.backoffice.api.transaction.dto.{TxnToUpdateForCancellation, TxnToUpdateForReversal}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionManagement
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class TransactionController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    transactionManagement: TransactionManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with api.transaction.TransactionController {

  import ApiController._
  import RequiredHeaders._
  import tech.pegb.backoffice.api.ApiErrors._

  implicit val executionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout = appConfig.FutureTimeout
  val PaginationMaxCap = appConfig.PaginationMaxLimit

  def getTransactionById(id: String): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      criteria ← EitherT.fromEither[Future]((id.toOption, None, None, None, None, None, None, None, None, None, Set[String]()).asDomain
        .toEither.leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to fetch transactions. A query parameter might have an empty value, wrong format or not among the expected values.".toOption)))

      total ← EitherT(transactionManagement.countTransactionsByCriteria(criteria)
        .map(_.leftMap(_.asApiError("Failed to count transactions".toOption))))

      results ← EitherT(transactionManagement.getTransactionsByCriteria(criteria, Constants.defaultOrdering.asDomain, None, None)
        .map(_.leftMap(_.asApiError())))

      maybeCancelledReason ← EitherT.fromEither[Future](transactionManagement.getTxnCancellationMetadata(id)
        .leftMap(_.asApiError("Failed to get transaction cancellation data".toOption)))

      maybeReversedReason ← EitherT.fromEither[Future](transactionManagement.getTxnReversalMetadata(id)
        .leftMap(_.asApiError("Failed to get transaction reversal data".toOption)))

    } yield {
      val anyReason = Seq(maybeCancelledReason, maybeReversedReason).flatten.headOption.map(_.reason)

      PaginatedResult(total, results.map(_.asApi(anyReason)), None, None).toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def getTransactions(
    anyCustomerName: Option[String],
    customerId: Option[UUIDLike],
    accountId: Option[UUIDLike],
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    `type`: Option[String],
    channel: Option[String],
    status: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId = getRequestId

    (for {
      _ ← EitherT.fromEither[Future](
        (dateFrom, dateTo, limit) match {
          case (Some(from), Some(to), _) if (from.localDateTime.isAfter(to.localDateTime)) ⇒
            "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft
          case (_, _, Some(lim)) if (lim > PaginationMaxCap) ⇒
            s"Limit provided(${limit.get}) is greater than PaginationMaxCap ${PaginationMaxCap}"
              .asInvalidRequestApiError.toLeft
          case _ ⇒ (dateFrom, dateTo, limit).toRight
        })

      partialMatchSet ← EitherT.fromEither[Future](
        partialMatch.validatePartialMatch(Constants.getTransactionsPartialMatchFields)
          .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](
        orderBy.orElse(Some(Constants.defaultOrdering)).validateOrdering(Constants.validOrderByFields)
          .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((None, anyCustomerName, customerId, accountId, dateFrom, dateTo, `type`, channel, status, None, partialMatchSet).asDomain
        .toEither.leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to fetch transactions. A query parameter might have an empty value, wrong format or not among the expected values.".toOption)))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("Failed getting the latest version of transactions".toOption))))

      total ← EitherT(executeIfGET(transactionManagement.countTransactionsByCriteria(criteria)
        .map(_.leftMap(_.asApiError("Failed to count transactions".toOption)))
        .futureWithTimeout
        .recover {
          case e: Throwable ⇒
            logger.warn("Counting transactions timed out. Returning total = -1")
            Right(-1)
        }, NoCount.toFuture))

      results ← EitherT(executeIfGET(transactionManagement.getTransactionsByCriteria(criteria, ordering, limit, offset)
        .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      (PaginatedResult(total, results.map(_.asApi()), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }

  }

  def cancelTransaction(id: String): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      dto ← EitherT.fromEither[Future](ctx.body.as(classOf[TxnToUpdateForCancellation], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()).map(_.asDomain(id.sanitize, getRequestFrom, getRequestDate)))

      result ← EitherT(transactionManagement.cancelTransaction(dto)
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.map(_.asApi(dto.reason.toOption)).toJsonStr

    }).value.map(handleApiResponse(_))
  }

  def revertTransaction(id: String): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      dto ← EitherT.fromEither[Future](ctx.body.as(classOf[TxnToUpdateForReversal], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()).map(_.asDomain(id.sanitize, getRequestFrom, getRequestDate)))

      result ← EitherT(transactionManagement.revertTransaction(dto)
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.map(_.asApi(dto.reason)).toJsonStr

    }).value.map(handleApiResponse(_))

  }
}
