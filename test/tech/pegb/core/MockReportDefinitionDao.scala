package tech.pegb.core

import com.google.inject.Singleton
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.report.abstraction.ReportDefinitionDao
import tech.pegb.backoffice.dao.report.dto.{ReportDefinitionCriteria, ReportDefinitionToInsert, ReportDefinitionToUpdate}

//Note: purpose of this is to avoid injection error in CashFlowReportService during unit test

@Singleton
class MockReportDefinitionDao extends ReportDefinitionDao {
  def createReportDefinition(reportDefinitionToInsert: ReportDefinitionToInsert, scopeToInsert: ScopeToInsert) = ???

  def getReportDefinitionByCriteria(criteria: ReportDefinitionCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]) = Right(Nil)

  def countReportDefinitionByCriteria(criteria: ReportDefinitionCriteria) = ???

  def getReportDefinitionById(id: String) = Right(None)

  def deleteReportDefinitionById(reportDefId: String, scopeId: Option[String], permissionIds: Seq[String]) = ???

  def updateReportDefinitionById(id: String, reportDefinitionToUpdate: ReportDefinitionToUpdate) = ???

  def getReportDefinitionPermissionByBackOfficeUserName(backOfficeUserName: String) = ???
}
