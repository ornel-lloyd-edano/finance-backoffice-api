package tech.pegb.backoffice.api.aggregations.dto

import play.api.libs.json.{Format, Json}

case class InstitutionUserBalancePercentage(
    name: String,
    percentage: BigDecimal)

object InstitutionUserBalancePercentage {
  implicit val format: Format[InstitutionUserBalancePercentage] = Json.format[InstitutionUserBalancePercentage]
}
