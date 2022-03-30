package tech.pegb.backoffice.domain.currencyrate.model

case class Rate(
    code: String,
    description: Option[String],
    buyRate: ExchangeRate,
    sellRate: ExchangeRate)
