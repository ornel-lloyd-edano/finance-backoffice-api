package tech.pegb.backoffice.mapping.domain.dao.report

import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.report.dto
import tech.pegb.backoffice.dao.report.dto.ReportDefinitionToInsert
import tech.pegb.backoffice.domain.report.dto.{ReportDefinitionCriteria, ReportDefinitionToCreate, ReportDefinitionToUpdate}

object Implicits {

  implicit class ReportDefinitionToCreateAdapter(val arg: ReportDefinitionToCreate) extends AnyVal {
    def asDao = ReportDefinitionToInsert(
      name = arg.name,
      title = arg.title,
      description = arg.description,
      columns = arg.columns.map(_.toString),
      parameters = arg.parameters.map(_.toString),
      joins = arg.joins.map(_.toString),
      grouping = arg.grouping.map(_.toString),
      ordering = arg.ordering.map(_.toString),
      paginated = arg.paginated,
      sql = arg.sql,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt)
  }

  implicit class ReportDefinitionToUpdateAdapter(val arg: ReportDefinitionToUpdate) extends AnyVal {
    def asDao = dto.ReportDefinitionToUpdate(
      title = arg.title,
      description = arg.description,
      columns = arg.columns.map(_.toString),
      parameters = arg.parameters.map(_.toString),
      joins = arg.joins.map(_.toString),
      grouping = arg.grouping.map(_.toString),
      ordering = arg.ordering.map(_.toString),
      paginated = arg.paginated,
      sql = arg.sql,
      updatedAt = arg.updatedAt,
      updatedBy = arg.updatedBy,
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class ReportDefinitionCriteriaAdapter(val arg: ReportDefinitionCriteria) extends AnyVal {
    def asDao = dto.ReportDefinitionCriteria(
      id = arg.id.map(i ⇒ CriteriaField("id", i,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      name = arg.name,
      title = arg.title,
      description = arg.description.map(d ⇒ CriteriaField("description", d,
        if (arg.partialMatchFields.contains("description")) MatchTypes.Partial else MatchTypes.Exact)))
  }

}
