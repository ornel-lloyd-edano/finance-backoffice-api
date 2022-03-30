package tech.pegb.backoffice.dao.report.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.report.dto.{ReportDefinitionCriteria, ReportDefinitionPermission, ReportDefinitionToInsert, ReportDefinitionToUpdate}
import tech.pegb.backoffice.dao.report.entity.ReportDefinition
import tech.pegb.backoffice.dao.report.sql.ReportDefinitionSqlDao

@ImplementedBy(classOf[ReportDefinitionSqlDao])
trait ReportDefinitionDao {

  def createReportDefinition(reportDefinitionToInsert: ReportDefinitionToInsert, scopeToInsert: ScopeToInsert): DaoResponse[ReportDefinition]

  def getReportDefinitionByCriteria(criteria: ReportDefinitionCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[ReportDefinition]]

  def countReportDefinitionByCriteria(criteria: ReportDefinitionCriteria): DaoResponse[Int]

  def getReportDefinitionById(id: String): DaoResponse[Option[ReportDefinition]]

  def deleteReportDefinitionById(reportDefId: String, scopeId: Option[String], permissionIds: Seq[String]): DaoResponse[Boolean]

  def updateReportDefinitionById(id: String, reportDefinitionToUpdate: ReportDefinitionToUpdate): DaoResponse[Option[ReportDefinition]]

  def getReportDefinitionPermissionByBackOfficeUserName(backOfficeUserName: String): DaoResponse[Seq[ReportDefinitionPermission]]
}
