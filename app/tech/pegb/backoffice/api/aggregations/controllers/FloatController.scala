package tech.pegb.backoffice.api.aggregations.controllers

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.aggregations.dto.{FloatTotals, InstitutionFloatSummary, UserBalancePercentageToRead}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}

@ImplementedBy(classOf[impl.FloatController])
@Api(value = "Float Dashboard", produces = "application/json")
trait FloatController {

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[FloatTotals], message = "")))
  def getTotalAggregations(currencyCode: String): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[InstitutionFloatSummary], message = "")))
  def getInstitutionStats(currencyCode: String): Action[AnyContent]

  def getInstitutionTrendsGraph(
    @ApiParam(required = true) institution: String,
    @ApiParam(required = true) currencyCode: String,
    @ApiParam(required = true) dateFrom: Option[LocalDateTimeFrom],
    @ApiParam(required = true) dateTo: Option[LocalDateTimeTo],
    @ApiParam(required = true, defaultValue = "daily", allowableValues = "daily, weekly, monthly", allowMultiple = false) frequency: String): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[UserBalancePercentageToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.aggregations.dto.UserBalancePercentageToUpdate",
      example = "", //not showing correctly
      paramType = "body",
      name = "UserBalancePercentageToUpdate")))
  def updatePercentage(institution: String): Action[String]

}
