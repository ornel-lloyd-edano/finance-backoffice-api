package tech.pegb.backoffice.api.currencyrate.dto

import java.time.ZonedDateTime

import com.fasterxml.jackson.annotation.JsonProperty

case class MainCurrency(
    mainCurrency: CurrencyRateToUpdate,
    rates: Seq[Rate],
    @JsonProperty("updated_at") lastUpdatedAt: Option[ZonedDateTime])
