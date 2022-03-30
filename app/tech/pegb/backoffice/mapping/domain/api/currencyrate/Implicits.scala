package tech.pegb.backoffice.mapping.domain.api.currencyrate

import tech.pegb.backoffice.api.currencyrate.dto.CurrencyRateToRead.{CurrencyRateToRead, ExchangeRate, MainCurrency, Rate}
import tech.pegb.backoffice.domain.currencyrate.model.CurrencyRate
import tech.pegb.backoffice.domain.financial.Implicits._

object Implicits {

  implicit class CurrencyRateAdapter(currencyRate: CurrencyRate) {
    def asApi: CurrencyRateToRead = {
      val rates = currencyRate.rates.map { cr ⇒
        Rate(
          code = cr.code,
          description = cr.description,
          buyRate = ExchangeRate(id = cr.buyRate.id, rate = cr.buyRate.rate.toFinancialFxRate),
          sellRate = ExchangeRate(id = cr.sellRate.id, rate = cr.sellRate.rate.toFinancialFxRate))
      }

      CurrencyRateToRead(
        mainCurrency = MainCurrency(
          id = currencyRate.mainCurrency.id,
          code = currencyRate.mainCurrency.code,
          description = currencyRate.mainCurrency.description),
        rates = rates)
    }
  }
}
