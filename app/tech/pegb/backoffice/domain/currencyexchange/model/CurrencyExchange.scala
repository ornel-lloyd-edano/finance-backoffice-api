package tech.pegb.backoffice.domain.currencyexchange.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

case class CurrencyExchange(
    id: UUID,
    currency: Currency,
    baseCurrency: Currency,
    rate: BigDecimal,
    provider: String,
    balance: BigDecimal,
    dailyAmount: Option[BigDecimal],
    status: String,
    lastUpdated: Option[LocalDateTime]) {
}

object CurrencyExchange {
  //TODO create factory method that accepts strings/doubles as args
  //def apply(...)

  val empty = new CurrencyExchange(id = UUID.randomUUID(), currency = java.util.Currency.getInstance("USD"),
    baseCurrency = java.util.Currency.getInstance("AED"), rate = BigDecimal(0), provider = "",
    balance = BigDecimal(0), dailyAmount = None, status = "active",
    lastUpdated = None)
}
