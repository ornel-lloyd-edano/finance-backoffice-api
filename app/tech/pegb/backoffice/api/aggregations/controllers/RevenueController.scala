package tech.pegb.backoffice.api.aggregations.controllers

import com.google.inject.ImplementedBy
import io.swagger.annotations.{Api, ApiResponse, ApiResponses}
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.aggregations.dto.{RevenueAggregation, RevenueSummaryToRead, TransactionTypeTotals}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}

@ImplementedBy(classOf[impl.RevenueController])
@Api(value = "Revenue", produces = "application/json")
trait RevenueController {

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[RevenueSummaryToRead], message = "")))
  def getAllRevenue(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[RevenueAggregation], message = "")))
  def getRevenueByAggregationType(
    aggregation: String,
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[TransactionTypeTotals], message = "")))
  def getTransactionTotals(
    currencyCode: String,
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo]): Action[AnyContent]
}
