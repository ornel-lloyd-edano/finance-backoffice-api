package tech.pegb.backoffice.api.aggregations.dto

case class InstitutionTrendGraph(
    cashIn: Seq[TimePeriodData],
    transactions: Seq[TimePeriodData],
    cashOut: Seq[TimePeriodData],
    closingUserBalance: Seq[TimePeriodData])
