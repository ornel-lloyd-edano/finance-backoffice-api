package tech.pegb.backoffice.dao.aggregations.dto

import tech.pegb.backoffice.dao.aggregations.abstraction.ScalarFunction

case class GroupByInput(column: String, scalarFunction: Option[ScalarFunction], alias: Option[String]) {

}
