package tech.pegb.backoffice.api.aggregations.controllers

import com.google.inject.ImplementedBy
import io.swagger.annotations.{Api, ApiParam, ApiResponse, ApiResponses}
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.aggregations.dto.{CashFlowTotals, Margin, TrendDirectionDetail}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.aggregations.controllers.impl.{AmountAggregationsController ⇒ AmountAggregationsControllerImpl}
import tech.pegb.backoffice.api.swagger.model.AmountAggregationPaginatedResult

@ImplementedBy(classOf[AmountAggregationsControllerImpl])
@Api(value = "AmountAggregations", produces = "application/json")
trait AmountAggregationsController {

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[AmountAggregationPaginatedResult], message = "")))
  def getAmountAggregation(
    @ApiParam(required = true, defaultValue = "turnover", allowableValues = "turnover, gross_revenue, third_party_fees, balance", allowMultiple = false) aggregation: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    @ApiParam(required = false, defaultValue = "", allowableValues = "cash_in, cash_out, etc_transactions, p2p, remittance, exchange, split_bill", allowMultiple = false) transactionType: Option[String],
    @ApiParam(required = false, defaultValue = "", allowableValues = "collection, distribution, end_customer", allowMultiple = false) accountType: Option[String],
    institution: Option[String],
    @ApiParam(required = false, defaultValue = "", allowableValues = "provider, individual, business", allowMultiple = false) userType: Option[String],
    @ApiParam(required = false, defaultValue = "", allowMultiple = false) notLikeThisAccountNumber: Option[String],
    @ApiParam(required = false, defaultValue = "daily", allowableValues = "daily, weekly, monthly", allowMultiple = false) frequency: Option[String],
    @ApiParam(required = false, defaultValue = "1") step: Option[Int],
    @ApiParam(required = false, allowableValues = "institution, transaction_type, trend_item, time_period", allowMultiple = false) groupBy: Option[String],
    @ApiParam(required = false, allowableValues = "aggregation, amount, institution, transaction_type, time_period", allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Margin], message = "")))
  def getGrossRevenueMargin(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    transactionType: Option[String],
    institution: Option[String],
    @ApiParam(required = false, defaultValue = "daily", allowableValues = "daily, weekly, monthly", allowMultiple = false) frequency: Option[String],
    @ApiParam(required = false, defaultValue = "1.0", allowableValues = "range[0, 1.0]", allowMultiple = false) frequencyReductionFactor: Option[Float],
    @ApiParam(required = false, allowableValues = "institution, transaction_type, time_period", allowMultiple = false) groupBy: Option[String],
    @ApiParam(required = false, allowableValues = "margin, institution, transaction_type, time_period", allowMultiple = false) orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TrendDirectionDetail], message = "")))
  def getTrendDirection(
    currencyCode: String,
    aggregation: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]): Action[AnyContent]

}
