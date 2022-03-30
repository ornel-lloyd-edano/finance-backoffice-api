package tech.pegb.backoffice.domain.currencyrate.model

import tech.pegb.backoffice.domain.currency.model.Currency

case class CurrencyRate(
    mainCurrency: Currency,
    rates: Seq[Rate])
