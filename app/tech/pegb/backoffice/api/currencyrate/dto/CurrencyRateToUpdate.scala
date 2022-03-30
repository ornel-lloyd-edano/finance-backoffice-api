package tech.pegb.backoffice.api.currencyrate.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class CurrencyRateToUpdate(
    id: Int,
    code: String,
    @JsonProperty(required = false) description: Option[String])

