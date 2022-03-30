package tech.pegb.backoffice.dao.aggregations.dto

case class AggregationResult(aggregations: Seq[AggregatedValue], grouping: Seq[Grouping]) {

}
