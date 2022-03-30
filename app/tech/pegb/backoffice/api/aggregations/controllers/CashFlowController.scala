package tech.pegb.backoffice.api.aggregations.controllers

import com.google.inject.ImplementedBy
import io.swagger.annotations.{Api, ApiResponse, ApiResponses}
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.aggregations.dto.{CashFlowAggregation}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.swagger.model.PaginatedCashFlowReport

@ImplementedBy(classOf[impl.CashFlowController])
@Api(value = "CashFlow Dashboard", produces = "application/json")
trait CashFlowController extends CustomReportRoute {

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CashFlowAggregation], message = "")))
  def getCashFlowAggregation(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    currency: String,
    institution: Option[String]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[PaginatedCashFlowReport], message = "")))
  def getCashFlowReport(
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    currency: Option[String],
    institution: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent]
}
