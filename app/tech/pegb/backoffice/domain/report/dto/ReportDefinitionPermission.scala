package tech.pegb.backoffice.domain.report.dto

case class ReportDefinitionPermission(
    reportDefId: String,
    reportDefName: String,
    reportDefTitle: String,
    scopeId: String,
    businessUnitId: String,
    roleId: String) {

}
