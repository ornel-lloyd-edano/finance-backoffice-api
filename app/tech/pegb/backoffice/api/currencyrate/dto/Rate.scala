package tech.pegb.backoffice.api.currencyrate.dto

case class Rate(
    code: String,
    description: Option[String],
    buyRate: ExchangeRate,
    sellRate: ExchangeRate)
