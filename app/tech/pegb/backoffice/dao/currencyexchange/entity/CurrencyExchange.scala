package tech.pegb.backoffice.dao.currencyexchange.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class CurrencyExchange(
    id: Long,
    uuid: String,
    currencyId: Long,
    currencyCode: String,
    baseCurrencyId: Long,
    baseCurrency: String,
    rate: BigDecimal,
    providerId: Int,
    provider: String,
    targetCurrencyAccountId: Long,
    targetCurrencyAccountUuid: String,
    baseCurrencyAccountId: Long,
    baseCurrencyAccountUuid: String,
    balance: BigDecimal,
    status: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String])

object CurrencyExchange {
  implicit val f = Json.format[CurrencyExchange]
}
