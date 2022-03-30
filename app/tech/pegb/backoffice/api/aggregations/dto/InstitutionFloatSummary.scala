package tech.pegb.backoffice.api.aggregations.dto

case class InstitutionFloatSummary(
    name: String,
    distributionAccountBalance: BigDecimal,
    institutionUserBalancePercentage: BigDecimal,
    calculatedUserBalance: BigDecimal,
    pendingBalance: BigDecimal)
