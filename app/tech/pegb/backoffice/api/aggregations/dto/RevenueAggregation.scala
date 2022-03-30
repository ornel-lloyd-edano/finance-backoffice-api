package tech.pegb.backoffice.api.aggregations.dto

case class RevenueAggregation(
    totalAmount: BigDecimal,
    margin: Seq[BigDecimal],
    data: Seq[TimePeriodData])

