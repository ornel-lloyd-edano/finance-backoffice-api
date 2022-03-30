package tech.pegb.backoffice.api.aggregations.controllers.impl

import java.util.{Currency, UUID}

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.aggregations.controllers.{Constants, AmountAggregationsController ⇒ AmountAggregationsControllerTrait}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api.{ApiController, ApiError, RequiredHeaders}
import tech.pegb.backoffice.domain.aggregations.abstraction.{RevenueMarginCalculator, TransactionAggregationFactory}
import tech.pegb.backoffice.domain.model
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.report.abstraction.CashFlowReportService
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.aggregation.Implicits._
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.aggregation.Implicits._
import tech.pegb.backoffice.mapping.api.domain.report.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AmountAggregationsController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    txnAggregationFactory: TransactionAggregationFactory,
    revenueMarginCalculator: RevenueMarginCalculator,
    cashFlowReportService: CashFlowReportService,
    implicit val appConfig: AppConfig)

  extends ApiController(controllerComponents) with RequiredHeaders with AmountAggregationsControllerTrait {

  import tech.pegb.backoffice.api.ApiErrors._
  import ApiController._
  import RequiredHeaders._

  private implicit val ec: ExecutionContext = executionContexts.genericOperations

  def getAmountAggregation(
    aggregation: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    transactionType: Option[String],
    accountType: Option[String],
    institution: Option[String],
    userType: Option[String],
    notLikeThisAccountNumber: Option[String],
    frequency: Option[String],
    step: Option[Int],
    groupBy: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    println(s"amount aggregation called [$aggregation]")

    (for {
      ordering ← EitherT(validate(currencyCode, dateFrom, dateTo, transactionType, accountType, institution, userType, frequency, aggregation.some, none, groupBy, orderBy, limit))

      aggFunction ← EitherT.fromEither[Future](txnAggregationFactory.getAggregationFunction(aggregation.trim.toLowerCase).toEither
        .leftMap(_ ⇒ s"Aggregation not found [$aggregation]".asInvalidRequestApiError))

      criteria ← EitherT.fromEither[Future](
        (None, None, None, None, dateFrom, dateTo, transactionType, None, None, currencyCode.some, Set[String]()).asDomain
          .toEither.leftMap(_.log().asInvalidRequestApiError("Failed to get latest version for transaction aggregation".some)))

      latestVersion ← {
        EitherT(latestVersionService.getLatestVersion(criteria)
          .map(_.leftMap(_.asApiError("Failed getting the latest version of transaction aggregation".toOption))))
      }

      result ← {
        //val criteria = (currencyCode, institution, transactionType, accountType, userType, dateFrom, dateTo).asDomain
        val criteria = (currencyCode, institution, transactionType, accountType, userType, dateFrom, dateTo, notLikeThisAccountNumber).asDomain
        val grouping = groupBy.map(_.asDomain(frequency))
        EitherT(aggFunction.apply(criteria, grouping, ordering, None, None)).leftMap(_.asApiError())
      }
    } yield {

      val manuallyPaginatedResult =
        result
          .map(_.asApi(aggregation)).sorted
          .drop(offset.getOrElse(0))
          .take(limit.getOrElse(appConfig.PaginationMaxLimit))

      (PaginatedResult(result.size, manuallyPaginatedResult, limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def getGrossRevenueMargin(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    transactionType: Option[String],
    institution: Option[String],
    frequency: Option[String],
    frequencyReductionFactor: Option[Float],
    groupBy: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT(validate(currencyCode, dateFrom, dateTo, transactionType, None, institution, None, frequency, none, frequencyReductionFactor, groupBy, orderBy, limit))

      criteria ← EitherT.fromEither[Future](
        (None, None, None, None, dateFrom, dateTo, transactionType, None, None, currencyCode.some, Set[String]()).asDomain
          .toEither.leftMap(_.log().asInvalidRequestApiError("Failed to get latest version for transaction aggregation".some)))

      latestVersion ← {
        EitherT(latestVersionService.getLatestVersion(criteria)
          .map(_.leftMap(_.asApiError("Failed getting the latest version of transaction aggregation".toOption))))
      }

      result ← {
        val criteria = (currencyCode, institution, transactionType, None, None, dateFrom, dateTo, None).asDomain
        val grouping = groupBy.map(_.asDomain(frequency))
        EitherT(revenueMarginCalculator.getRevenueMargin(None, None, criteria, grouping, ordering, None, None)).leftMap(_.asApiError())
      }
    } yield {

      val manuallyPaginatedResult = result.map(_.asApi).sorted
        .drop(offset.getOrElse(0))
        .take(limit.getOrElse(appConfig.PaginationMaxLimit))
      (PaginatedResult(result.size, manuallyPaginatedResult, limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  private def validate(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    transactionType: Option[String],
    accountType: Option[String],
    institution: Option[String],
    userType: Option[String],
    frequency: Option[String],
    mayBeAggregation: Option[String],
    frequencyReductionFactor: Option[Float],
    groupBy: Option[String],
    orderBy: Option[String],
    limit: Option[Int])(implicit requestId: UUID): Future[Either[ApiError, Seq[model.Ordering]]] = {
    (for {
      //validate currency code
      _ ← EitherT.fromEither[Future](Try(Currency.getInstance(currencyCode))
        .toEither.leftMap(_.log().asInvalidRequestApiError(s"Invalid currency_code [$currencyCode]".some)))

      //validate date range
      _ ← EitherT.fromEither[Future](
        (dateFrom, dateTo) match {
          case (Some(from), Some(to)) if (from.localDateTime.isAfter(to.localDateTime)) ⇒
            "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft
          case _ ⇒ (dateFrom, dateTo).toRight
        })

      //validate transaction type
      _ ← EitherT.fromEither[Future](transactionType.fold[Either[ApiError, Unit]](Right(())) { txnType ⇒
        if (!Constants.ValidTxnType.lenientContains(txnType)) {
          s"Transaction type [$txnType] is not valid for aggregation. Valid transaction_type values ${Constants.ValidTxnType.defaultMkString}.".asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        }
      })

      // validate account type
      _ ← EitherT.fromEither[Future](accountType.fold[Either[ApiError, Unit]](Right(())) { accType ⇒
        if (!Constants.ValidAccntType.lenientContains(accType)) {
          s"Account type [$accType] is not valid for aggregation. Valid account_type values ${Constants.ValidAccntType.defaultMkString}.".asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        }
      })

      //validate userTypes
      _ ← EitherT.fromEither[Future](userType.fold[Either[ApiError, Unit]](Right(())) { userType ⇒
        if (!Constants.ValidUserType.lenientContains(userType)) {
          s"Transaction type [$userType] is not valid for aggregation. Valid transaction_type values ${Constants.ValidUserType.defaultMkString}.".asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        }
      })

      //validate frequency
      _ ← EitherT.fromEither[Future](frequency.fold[Either[ApiError, Unit]](Right(())) { frequency ⇒
        if (!Constants.ValidFrequency.lenientContains(frequency)) {
          s"Frequency [$frequency] is not valid for aggregation. Valid frequency values [${Constants.ValidFrequency.mkString(",")}].".asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        }
      })

      //validate groupBy
      _ ← EitherT.fromEither[Future](groupBy.map(_.toSeqByComma).fold[Either[ApiError, Unit]](Right(Nil)) { groupings ⇒
        if (!Constants.ValidGroupBy.lenientContainsAll(groupings)) {
          s"Grouping by [${groupings.mkString(",")}] is not valid for aggregation. Valid group_by values ${Constants.ValidGroupBy.defaultMkString}.".asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        }
      })

      //validate orderBy
      ordering ← EitherT.fromEither[Future](
        orderBy.validateOrdering(Constants.ValidOrderBy)
          .leftMap(_.log().asInvalidRequestApiError()))

      //validate max limit
      _ ← EitherT.fromEither[Future](limit.fold[Either[ApiError, Unit]](Right(())) { limit ⇒
        if (limit > appConfig.PaginationMaxLimit) {
          s"Limit provided(${limit}) is greater than PaginationMaxCap ${appConfig.PaginationMaxLimit}"
            .asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        }
      })

      //validate aggregation
      _ ← EitherT.fromEither[Future](mayBeAggregation.fold[Either[ApiError, Unit]](().toRight) { aggregation ⇒
        (if (!Constants.ValidAggregations.lenientContains(aggregation)) {
          s"Aggregation [$aggregation] is not valid".asInvalidRequestApiError.toLeft
        } else {
          ().toRight
        })
      })
    } yield ordering).value

  }

  def getTrendDirection(
    currencyCode: String,
    aggregation: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]) = ???
}
