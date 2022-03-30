package tech.pegb.backoffice.dao.report.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.report.sql.ReportDefinitionSqlDao._

case class ReportDefinitionToUpdate(
    title: String,
    description: String,
    columns: Option[String] = None,
    parameters: Option[String] = None,
    joins: Option[String] = None,
    grouping: Option[String] = None,
    ordering: Option[String] = None,
    paginated: Boolean,
    sql: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime] = None) extends GenericUpdateSql {

  //lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x) //uncomment if optimistic logging is needed

  append(cTitle → title)
  append(cDescription → description)

  columns.foreach(x ⇒ append(cColumns → x))
  parameters.foreach(x ⇒ append(cParameters → x))
  joins.foreach(x ⇒ append(cJoins → x))
  grouping.foreach(x ⇒ append(cGrouping → x))
  ordering.foreach(x ⇒ append(cOrdering → x))

  append(cPaginated → paginated)
  append(cSql → sql)
  append(cUpdatedBy → updatedBy)
  append(cUpdatedAt → updatedAt)

}
