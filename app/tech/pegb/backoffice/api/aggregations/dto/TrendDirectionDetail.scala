package tech.pegb.backoffice.api.aggregations.dto

import java.time.ZonedDateTime

case class TrendDirectionDetail(
    direction: String,
    highest: Option[(BigDecimal, ZonedDateTime)],
    lowest: Option[(BigDecimal, ZonedDateTime)],
    average: Option[BigDecimal]) {

}
