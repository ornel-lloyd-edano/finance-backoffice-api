package tech.pegb.backoffice.mapping.dao.domain.report

import java.util.UUID

import play.api.libs.json.{JsArray, Json}
import tech.pegb.backoffice.dao.report.dto.ReportDefinitionPermission
import tech.pegb.backoffice.dao.report.entity.{Report, ReportDefinition}
import tech.pegb.backoffice.domain

import scala.util.Try

object Implicits {
  implicit class ReportDefinitionAdapter(val arg: ReportDefinition) extends AnyVal {
    def asDomain = Try {
      domain.report.model.ReportDefinition(
        id = UUID.fromString(arg.id),
        name = arg.name,
        title = arg.title,
        description = arg.description,
        columns = arg.columns.map(Json.parse(_).as[JsArray]),
        parameters = arg.parameters.map(Json.parse(_).as[JsArray]),
        joins = arg.joins.map(Json.parse(_).as[JsArray]),
        grouping = arg.grouping.map(Json.parse(_).as[JsArray]),
        ordering = arg.ordering.map(Json.parse(_).as[JsArray]),
        paginated = arg.paginated,
        sql = arg.sql,
        createdAt = arg.createdAt,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy)
    }
  }

  implicit class ReportAdapter(val arg: Report) extends AnyVal {
    def asDomain = {
      domain.report.model.Report(
        count = arg.count,
        result = arg.result)
    }
  }

  implicit class ReportDefinitionPermissionAdapter(val arg: ReportDefinitionPermission) extends AnyVal {
    def asDomain = {
      domain.report.dto.ReportDefinitionPermission(
        reportDefId = arg.reportDefId,
        reportDefName = arg.reportDefName,
        reportDefTitle = arg.reportDefTitle,
        scopeId = arg.scopeId,
        businessUnitId = arg.businessUserId,
        roleId = arg.roleId)
    }
  }
}
