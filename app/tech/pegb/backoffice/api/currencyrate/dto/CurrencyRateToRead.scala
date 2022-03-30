package tech.pegb.backoffice.api.currencyrate.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.swagger.annotations.ApiModelProperty

object CurrencyRateToRead {

  case class ExchangeRate(
      @ApiModelProperty(name = "id", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true) id: UUID,
      @ApiModelProperty(name = "rate", example = "0", required = true) rate: BigDecimal)

  case class Rate(
      @ApiModelProperty(name = "code", example = "currency code", required = true) code: String,
      @ApiModelProperty(name = "description", example = "currency desc", required = true) description: Option[String],
      @ApiModelProperty(name = "buy_rate", example = "rate", required = true) buyRate: ExchangeRate,
      @ApiModelProperty(name = "sell_rate", example = "rate", required = true) sellRate: ExchangeRate)

  case class MainCurrency(
      @ApiModelProperty(name = "id", example = "1L", required = true) id: Long,
      @ApiModelProperty(name = "code", example = "currency code", required = true) code: String,
      @ApiModelProperty(name = "description", example = "currency desc", required = true) description: Option[String])

  case class CurrencyRateToRead(
      @ApiModelProperty(name = "main_currency", example = "main currency", required = true) mainCurrency: MainCurrency,
      @ApiModelProperty(name = "rates", example = "rates", required = true) rates: Seq[Rate])

  case class CurrencyRateResultToRead(
      @ApiModelProperty(name = "updated_at", example = "", required = true) updatedAt: Option[ZonedDateTime],
      @ApiModelProperty(name = "results", example = "", required = true) results: Seq[CurrencyRateToRead])

}
