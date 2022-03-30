package tech.pegb.backoffice.api.aggregations.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc._
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.aggregations.controllers.Constants
import tech.pegb.backoffice.api.aggregations.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RevenueController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    val httpClient: HttpClient,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders
  with api.aggregations.controllers.RevenueController
  with AmountAggregationUtil {

  import Margin._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  val aggregationMarginURL = appConfig.AggregationEndpoints.aggregationMargin
  val shadowStep = appConfig.AggregationConstants.step

  def getAllRevenue(currencyCode: String, dateFrom: Option[LocalDateTimeFrom], dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val sanitizedCurrency = currencyCode.sanitize
    val turnoverF = getRevenueAggregation(Constants.Aggregations.TurnOver, sanitizedCurrency, dateFrom, dateTo, shadowStep.some)
    val grossRevenueF = getRevenueAggregation(Constants.Aggregations.GrossRevenue, sanitizedCurrency, dateFrom, dateTo, shadowStep.some)
    val thirdPartyFeeF = getRevenueAggregation(Constants.Aggregations.ThirdPartyFees, sanitizedCurrency, dateFrom, dateTo, shadowStep.some)

    (for {
      turnover ← EitherT(turnoverF)
      grossRevenue ← EitherT(grossRevenueF)
      thirdPartyFee ← EitherT(thirdPartyFeeF)
    } yield {

      RevenueSummaryToRead(
        turnover = turnover,
        grossRevenue = grossRevenue,
        thirdPartyFees = thirdPartyFee)
    }).fold(
      identity,
      summary ⇒ {

        handleApiResponse(Right(summary.toJsonStr))
      })
  }

  def getRevenueByAggregationType(
    aggregation: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    EitherT(getRevenueAggregation(aggregation, currencyCode.sanitize, dateFrom, dateTo)).fold(
      identity,
      revenueAggregation ⇒ handleApiResponse(Right(revenueAggregation.toJsonStr)))
  }

  def getTransactionTotals(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val sanitizedCurrency = currencyCode.sanitize
    val turnoverTxnTotalsF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.TurnOver,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      groupBy = "transaction_type".some)
    val grossRevenueTxnTotalsF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.GrossRevenue,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      groupBy = "transaction_type".some)
    val thirdPartyFeesTxnTotalsF = getAmountAggregationSeq(
      aggregation = Constants.Aggregations.ThirdPartyFees,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      groupBy = "transaction_type".some)

    (for {
      turnoverTxnTotals ← EitherT(turnoverTxnTotalsF)
      grossRevenueTxnTotals ← EitherT(grossRevenueTxnTotalsF)
      thirdPartyFeesTxnTotals ← EitherT(thirdPartyFeesTxnTotalsF)

      turnoverLookUpMap = turnoverTxnTotals.collect { case agg if agg.transactionType.isDefined ⇒ (agg.transactionType.get, agg) }.toMap
      grossRevenueLookUpMap = grossRevenueTxnTotals.collect { case agg if agg.transactionType.isDefined ⇒ (agg.transactionType.get, agg) }.toMap
      thirdPartyFeesLookUpMap = thirdPartyFeesTxnTotals.collect { case agg if agg.transactionType.isDefined ⇒ (agg.transactionType.get, agg) }.toMap
    } yield {
      val txnTypeSet = turnoverLookUpMap.keySet ++ grossRevenueLookUpMap.keySet ++ thirdPartyFeesLookUpMap.keySet

      txnTypeSet.map(txnType ⇒
        TransactionTypeTotals(
          transactionType = txnType,
          turnover = turnoverLookUpMap.get(txnType).map(_.amount).getOrElse(0),
          grossRevenue = grossRevenueLookUpMap.get(txnType).map(_.amount).getOrElse(0),
          thirdPartyFees = thirdPartyFeesLookUpMap.get(txnType).map(_.amount).getOrElse(0))).toSeq.sortBy(_.transactionType)
    }).fold(
      identity,
      txnTypeTotalSeq ⇒ handleApiResponse(Right(txnTypeTotalSeq.toJsonStr)))
  }

  private def getRevenueAggregation(
    aggregation: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    step: Option[Int] = None)(implicit requestId: UUID, ctx: Request[_]): Future[Either[Result, RevenueAggregation]] = {
    val sanitizedCurrency = currencyCode.sanitize
    val sanitizedAgg = aggregation.sanitize
    val revenueResultF = getAmountAggregationSeq(
      aggregation = sanitizedAgg,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo)
    val trendResultF = getAmountAggregationSeq(
      aggregation = sanitizedAgg,
      currencyCode = sanitizedCurrency,
      dateFrom = dateFrom,
      dateTo = dateTo,
      groupBy = "time_period".some,
      step = step)
    val marginF = if (aggregation == Constants.Aggregations.GrossRevenue) {
      val marginSb = new StringBuilder(aggregationMarginURL)
      marginSb.append(s"?currency_code=${sanitizedCurrency}")
      dateFrom.foreach(from ⇒ marginSb.append(s"&date_from=${from.localDateTime}"))
      dateTo.foreach(to ⇒ marginSb.append(s"&date_to=${to.localDateTime}"))

      EitherT(sendHttpCall[Seq[Margin]](marginSb.toString(), "results".some)).map(_.map(_.margin)).value
    } else {
      Future.successful(Seq.empty[BigDecimal].asRight[Result])
    }

    (for {
      revenueResult ← EitherT(revenueResultF)
      trendResult ← EitherT(trendResultF)
      margin ← EitherT(marginF)
    } yield {
      RevenueAggregation(
        totalAmount = revenueResult.headOption.map(_.amount).getOrElse(0),
        margin = margin,
        data = trendResult.map(d ⇒ TimePeriodData(
          timePeriod = d.timePeriod.getOrElse("UNKNOWN"),
          amount = d.amount)))
    }).value
  }

}

object RevenueController {
  def malformedPaginatedAmountAggregationResponse(url: String) = s"Malformed response received from url ${url}"

}
