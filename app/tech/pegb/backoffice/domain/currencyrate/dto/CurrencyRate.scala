package tech.pegb.backoffice.domain.currencyrate.dto

case class CurrencyRate(
    code: String,
    description: Option[String],
    buyRate: ExchangeRate,
    sellRate: ExchangeRate)
