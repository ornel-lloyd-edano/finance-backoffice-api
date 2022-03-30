package tech.pegb.backoffice.api.aggregations.dto

import play.api.libs.json.{Format, Json, JsonConfiguration}
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase

case class CashFlowReport(
    date: String,
    currency: String,
    institution: String,
    openingBalance: BigDecimal,
    bankTransfer: BigDecimal,
    cashIn: BigDecimal,
    cashOut: BigDecimal,
    otherTransactions: BigDecimal,
    closingBalance: BigDecimal) {
}

object CashFlowReportPaginatedResult {
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val cashFlowFormat: Format[CashFlowReport] = Json.format[CashFlowReport]
}

