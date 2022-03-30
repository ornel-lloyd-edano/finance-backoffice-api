package tech.pegb.backoffice.api.aggregations.dto

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Format, Json, JsonConfiguration}

case class AmountAggregation(
    aggregation: String,
    amount: BigDecimal,
    currencyCode: String,
    transactionType: Option[String],
    institution: Option[String],
    timePeriod: Option[String]) {
}

object AmountAggregation {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val format: Format[AmountAggregation] = Json.format[AmountAggregation]

  implicit def amountAggregationOrdering[A <: AmountAggregation]: Ordering[A] =
    Ordering.by(a ⇒ (a.currencyCode, a.transactionType, a.institution))
}
