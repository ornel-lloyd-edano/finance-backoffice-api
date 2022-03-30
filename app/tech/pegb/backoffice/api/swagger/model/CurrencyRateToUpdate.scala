package tech.pegb.backoffice.api.swagger.model

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class ExchangeRate(
    @ApiModelProperty(name = "id", required = true) id: String,
    @ApiModelProperty(name = "code", required = true) rate: BigDecimal)

case class Currency(
    @ApiModelProperty(name = "code", example = "currency code", required = true) code: String,
    @ApiModelProperty(name = "description", example = "currency desc", required = true) description: Option[String],
    @ApiModelProperty(name = "buy_rate", example = "buy rate", required = true) buyRate: ExchangeRate,
    @ApiModelProperty(name = "sell_rate", example = "sell rate", required = true) sellRate: ExchangeRate)

case class MainCurrency(
    @ApiModelProperty(name = "id", example = "1", required = true) id: Int,
    @ApiModelProperty(name = "code", example = "currency code", required = true) code: String,
    @ApiModelProperty(name = "description", example = "currency desc", required = true) description: Option[String])

case class CurrencyRateToUpdate(
    @ApiModelProperty(name = "main_currency", example = "", required = true) mainCurrency: MainCurrency,
    @ApiModelProperty(name = "rates", required = true) rates: Array[Currency],
    @ApiModelProperty(name = "updated_at", required = true) updatedAt: Option[ZonedDateTime])

