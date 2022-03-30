package tech.pegb.backoffice.dao.aggregations.dto

import tech.pegb.backoffice.dao.aggregations.abstraction.AggFunction

case class AggregationInput(columnOrExpression: String, function: AggFunction, alias: Option[String]) {

}
