package tech.pegb.backoffice.api.aggregations.dto

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Format, Json, JsonConfiguration}

case class Margin(
    margin: BigDecimal,
    currencyCode: String,
    transactionType: Option[String] = None,
    institution: Option[String] = None,
    timePeriod: Option[String] = None) {

}

object Margin {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val format: Format[Margin] = Json.format[Margin]

  implicit def marginAggregationOrdering[A <: Margin]: Ordering[A] =
    Ordering.by(m ⇒ (m.currencyCode, m.transactionType, m.institution))
}
