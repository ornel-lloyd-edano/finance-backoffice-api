package tech.pegb.backoffice.api.aggregations.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.aggregations.controllers.{Constants, CustomReportRoute}
import tech.pegb.backoffice.api.aggregations.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo, PaginatedResult}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.domain.financial.Implicits._
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.report.abstraction.CashFlowReportService
import tech.pegb.backoffice.mapping.api.domain.report.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.report.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.time.DateTimeRangeUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

@Singleton
class CashFlowController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    cashFlowReportService: CashFlowReportService,
    val httpClient: HttpClient,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with api.aggregations.controllers.CashFlowController
  with AmountAggregationUtil {

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  lazy val fixedReportId: UUID = appConfig.Reports.cashflowReportUuid

  def getCashFlowAggregation(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    currency: String,
    institution: Option[String]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    // TODO : validate dateFrom and dateTo

    val sanitizedCurrency = currency.sanitize
    val sanitizedInstitution = institution.map(_.sanitize)

    val bankTransferF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.ProviderTurnover,
      currencyCode = sanitizedCurrency,
      transactionType = Constants.TxnType.BankTransfer.some,
      institution = sanitizedInstitution,
      dateFrom = dateFrom,
      dateTo = dateTo,
      userType = Constants.UsersType.Provider.some)
    val totalCashInF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.ProviderTurnover,
      currencyCode = sanitizedCurrency,
      transactionType = Constants.TxnType.CashIn.some,
      institution = sanitizedInstitution,
      dateFrom = dateFrom,
      dateTo = dateTo,
      userType = Constants.UsersType.Provider.some)
    val totalCashOutF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.ProviderTurnover,
      currencyCode = sanitizedCurrency,
      transactionType = Constants.TxnType.CashOut.some,
      institution = sanitizedInstitution,
      dateFrom = dateFrom,
      dateTo = dateTo,
      userType = Constants.UsersType.Provider.some)
    val totalEtcTxnsF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.ProviderTurnover,
      currencyCode = sanitizedCurrency,
      transactionType = Constants.TxnType.EtcTxns.some,
      institution = sanitizedInstitution,
      dateFrom = dateFrom,
      dateTo = dateTo,
      userType = Constants.UsersType.Provider.some,
      // Relies on always following the convention of using pegb_fees.1 (or any number) to ignore for fees records
      notLikeThisAccountNumber = Constants.AccountNumber.FilterNotPegbFees.some)

    (for {
      _ ← EitherT.fromEither[Future] {
        DateTimeRangeUtil.validateDateTimeRange(
          dateFrom.map(_.localDateTime),
          dateTo.map(_.localDateTime))(appConfig.DateTimeRangeLimits.dateTimeRangeConfig.some)
          .leftMap(error ⇒ makeApiErrorResponse(error.asInvalidRequestApiError()))
      }
      bankTransfer ← EitherT(bankTransferF)
      totalCashIn ← EitherT(totalCashInF)
      totalCashOut ← EitherT(totalCashOutF)
      totalEtcTxns ← EitherT(totalEtcTxnsF)
    } yield {

      CashFlowAggregation(
        currency = sanitizedCurrency,
        totalBankTransfer = bankTransfer.headOption.map(_.amount).getOrElse(BigDecimal(0)).toFinancial,
        totalCashIn = totalCashIn.headOption.map(_.amount).getOrElse(BigDecimal(0)).toFinancial,
        totalCashOut = totalCashOut.headOption.map(_.amount).getOrElse(BigDecimal(0)).toFinancial,
        totalTxnEtc = totalEtcTxns.headOption.map(_.amount).getOrElse(BigDecimal(0)).toFinancial)
    }).fold(
      identity,
      agg ⇒ handleApiResponse(Right(agg.toJsonStr)))
  }

  def getCashFlowReport(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    currency: Option[String],
    institution: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      _ ← EitherT.fromEither[Future] {
        DateTimeRangeUtil.validateDateTimeRange(
          dateFrom.map(_.localDateTime),
          dateTo.map(_.localDateTime))(appConfig.DateTimeRangeLimits.dateTimeRangeConfig.some)
          .leftMap(_.asInvalidRequestApiError())
      }
      result ← EitherT {
        val criteria = (
          dateFrom.map(_.localDateTime.toLocalDate),
          dateTo.map(_.localDateTime.toLocalDate),
          currency, institution).asDomain
        cashFlowReportService.getCashFlowReport(criteria, Nil, limit, offset)
          .map(_.leftMap(_.asApiError()))
      }
    } yield {
      PaginatedResult(result.size, results = result.asApi(limit, offset), limit, offset).toJsonStr
    }).value.map(handleApiResponse(_))
  }
}
