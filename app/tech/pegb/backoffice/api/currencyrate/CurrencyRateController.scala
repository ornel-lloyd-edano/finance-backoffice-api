package tech.pegb.backoffice.api.currencyrate

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.currencyrate.dto.CurrencyRateToRead.{CurrencyRateResultToRead, CurrencyRateToRead}

@Api(value = "Currency Rates", produces = "application/json", consumes = "application/json")
@ImplementedBy(classOf[controllers.CurrencyRateController])
trait CurrencyRateController extends Routable {

  def getRoute: String = "currency_rates"

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyRateResultToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.CurrencyRateToUpdate",
      example = "",
      paramType = "body",
      name = "CurrencyRateToUpdate")))
  def update(id: Int): Action[String]

  @ApiOperation(value = "Returns list of currency_rates")
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyRateResultToRead], message = "")))
  def getCurrencyRate(orderBy: Option[String], showEmpty: Option[Boolean]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[CurrencyRateToRead], message = "")))
  def getCurrencyRateById(id: Long): Action[AnyContent]
}
