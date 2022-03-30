package tech.pegb.backoffice.domain.aggregations.dto

case class FeeRange(min: Option[BigDecimal], max: Option[BigDecimal], value: BigDecimal)
