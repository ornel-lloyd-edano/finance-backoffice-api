package tech.pegb.backoffice.api.transaction.dto

import java.time.ZonedDateTime

import io.swagger.annotations.ApiModelProperty

case class SettlementFxHistoryToRead(
    @ApiModelProperty(name = "fx_provider", example = "Central Bank of Kenya: CBK", required = true) fxProvider: String,
    @ApiModelProperty(name = "fx_provider", example = "KES", required = true) fromCurrency: String,
    @ApiModelProperty(name = "fx_provider", example = "kes_flag", required = true) fromFlag: String,
    @ApiModelProperty(name = "fx_provider", example = "USD", required = true) toCurrency: String,
    @ApiModelProperty(name = "fx_provider", example = "usd_flag", required = true) toFlag: String,
    @ApiModelProperty(name = "fx_rate", example = "0.09834", required = true) fxRate: BigDecimal,
    @ApiModelProperty(name = "created_at", example = "2020-01-06T13:43:12.125Z", required = true) createdAt: ZonedDateTime)
