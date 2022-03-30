package tech.pegb.backoffice.api.swagger.model

case class FloatAggregationPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.customer.dto.FloatAccountAggregationToRead])
