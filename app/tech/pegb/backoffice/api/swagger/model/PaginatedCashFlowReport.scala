package tech.pegb.backoffice.api.swagger.model

case class PaginatedCashFlowReport(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.aggregations.dto.CashFlowReport]) {

}
