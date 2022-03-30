package tech.pegb.backoffice.dao.transaction.dto

import java.time.LocalDateTime

case class SettlementToInsert(
    uuid: String,
    createdBy: String,
    createdAt: LocalDateTime,
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    status: String,
    reason: String,
    fxProvider: Option[String],
    fromCurrencyId: Option[Int],
    toCurrencyId: Option[Int],
    fxRate: Option[BigDecimal],
    settlementLines: Seq[SettlementLinesToInsert])

case class SettlementLinesToInsert(
    accountId: Int,
    direction: String,
    currencyId: Int,
    amount: BigDecimal,
    explanation: String)
