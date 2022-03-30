package tech.pegb.backoffice.dao.report.dto

case class ReportDefinitionPermission(
    reportDefId: String,
    reportDefTitle: String,
    reportDefName: String,
    scopeId: String,
    businessUserId: String,
    roleId: String) {

}
