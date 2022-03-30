package tech.pegb.backoffice.mapping.domain.api.currencyexchange

import tech.pegb.backoffice.api.currencyexchange.dto.{CurrencyExchangeToRead, CurrencyExchangeToReadDetails, SpreadToRead}
import tech.pegb.backoffice.domain.currencyexchange.model.{CurrencyExchange, Spread}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.financial.Implicits._

object Implicits {

  implicit class CurrencyExchangeAdapter(val arg: CurrencyExchange) extends AnyVal {
    def asApi: CurrencyExchangeToRead = {
      CurrencyExchangeToRead(
        id = arg.id,
        buyCurrency = arg.currency.getCurrencyCode,
        currencyDescription = arg.currency.getDisplayName,
        sellCurrency = arg.baseCurrency.getCurrencyCode,
        rate = arg.rate,
        provider = arg.provider,
        balance = arg.balance.toFinancial,
        status = arg.status,
        updatedAt = arg.lastUpdated.map(_.toZonedDateTimeUTC))
    }

    def asApiDetails: CurrencyExchangeToReadDetails = {
      CurrencyExchangeToReadDetails(
        id = arg.id,
        buyCurrency = arg.currency.getCurrencyCode,
        currencyDescription = arg.currency.getDisplayName,
        sellCurrency = arg.baseCurrency.getCurrencyCode,
        rate = arg.rate,
        provider = arg.provider,
        balance = arg.balance.toFinancial,
        dailyAmount = arg.dailyAmount.map(_.toFinancial),
        status = arg.status,
        updatedAt = arg.lastUpdated.map(_.toZonedDateTimeUTC))
    }
  }

  implicit class SpreadsAdapter(val arg: Spread) extends AnyVal {
    def asApi = SpreadToRead(
      id = arg.id,
      currencyExchangeId = arg.currencyExchange.id,
      buyCurrency = arg.currencyExchange.currency.getCurrencyCode,
      sellCurrency = arg.currencyExchange.baseCurrency.getCurrencyCode,
      transactionType = arg.transactionType.underlying,
      channel = arg.channel.map(_.underlying),
      institution = arg.recipientInstitution,
      spread = arg.spread,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }

}
