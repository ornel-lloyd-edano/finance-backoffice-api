package tech.pegb.backoffice.api.aggregations.dto

case class FloatTotals(
    institutionCollectionBalance: BigDecimal,
    institutionDistributionBalance: BigDecimal,
    userBalance: BigDecimal,
    pendingBalance: BigDecimal)
