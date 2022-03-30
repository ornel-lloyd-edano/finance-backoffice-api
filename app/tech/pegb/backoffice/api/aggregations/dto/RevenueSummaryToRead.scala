package tech.pegb.backoffice.api.aggregations.dto

case class RevenueSummaryToRead(
    turnover: RevenueAggregation,
    grossRevenue: RevenueAggregation,
    thirdPartyFees: RevenueAggregation)

