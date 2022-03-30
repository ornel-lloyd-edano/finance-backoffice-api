package tech.pegb.backoffice.dao.transaction.entity

import java.time.LocalDateTime

import play.api.libs.json.Json

case class SettlementLines(
    id: Int,
    manualSettlementId: Int,
    accountId: Int,
    accountNumber: String,
    direction: String,
    currencyId: Int,
    currency: String,
    amount: BigDecimal,
    explanation: String)

object SettlementLines {
  implicit val f = Json.format[SettlementLines]
}

case class Settlement(
    id: Int,
    uuid: String,
    transactionReason: String,
    createdBy: String,
    createdAt: LocalDateTime,
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    status: String,
    fxProvider: Option[String],
    fromCurrencyId: Option[Int],
    toCurrencyId: Option[Int],
    fxRate: Option[BigDecimal],
    lines: Seq[SettlementLines])

object Settlement {
  implicit val f = Json.format[Settlement]
}
