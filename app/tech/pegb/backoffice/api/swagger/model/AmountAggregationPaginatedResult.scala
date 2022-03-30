package tech.pegb.backoffice.api.swagger.model

import tech.pegb.backoffice.api.aggregations.dto.AmountAggregation

case class AmountAggregationPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[AmountAggregation]) {

}
