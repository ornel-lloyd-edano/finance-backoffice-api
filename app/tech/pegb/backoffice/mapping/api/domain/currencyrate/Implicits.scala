package tech.pegb.backoffice.mapping.api.domain.currencyrate

import java.time.ZonedDateTime

import tech.pegb.backoffice.api.currencyrate.dto.MainCurrency
import tech.pegb.backoffice.domain.currencyrate.dto.{CurrencyRate, ExchangeRate, CurrencyRateToUpdate ⇒ CurrencyRateToUpdateDomain}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class CurrencyRateUpdateConverter(mainCurrency: MainCurrency) {
    def asDomain(doneAt: ZonedDateTime, doneBy: String): CurrencyRateToUpdateDomain = {

      val currencyRateToUpdate = mainCurrency.mainCurrency
      val currencyRates = mainCurrency.rates.map { cr ⇒
        val buyExchangeRate = ExchangeRate(
          id = cr.buyRate.id,
          rate = cr.buyRate.rate)

        val sellExchangeRate = ExchangeRate(
          id = cr.sellRate.id,
          rate = cr.sellRate.rate)

        CurrencyRate(
          code = cr.code,
          description = cr.description,
          buyRate = buyExchangeRate,
          sellRate = sellExchangeRate)

      }

      CurrencyRateToUpdateDomain(
        name = currencyRateToUpdate.code,
        description = currencyRateToUpdate.description,
        rates = currencyRates,
        updatedAt = doneAt.toLocalDateTimeUTC,
        updatedBy = doneBy)
    }
  }

}
