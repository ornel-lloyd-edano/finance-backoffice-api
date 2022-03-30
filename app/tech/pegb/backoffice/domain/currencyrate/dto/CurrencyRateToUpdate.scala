package tech.pegb.backoffice.domain.currencyrate.dto

import java.time.LocalDateTime

case class CurrencyRateToUpdate(
    name: String,
    description: Option[String],
    rates: Seq[CurrencyRate],
    updatedAt: LocalDateTime,
    updatedBy: String)

