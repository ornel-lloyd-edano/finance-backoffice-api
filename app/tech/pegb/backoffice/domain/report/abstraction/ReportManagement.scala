package tech.pegb.backoffice.domain.report.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.model
import tech.pegb.backoffice.domain.report.dto.{ReportDefinitionCriteria, ReportDefinitionPermission, ReportDefinitionToCreate, ReportDefinitionToUpdate}
import tech.pegb.backoffice.domain.report.implementation.ReportMgmtService
import tech.pegb.backoffice.domain.report.model.{Report, ReportDefinition}

import scala.concurrent.Future

@ImplementedBy(classOf[ReportMgmtService])
trait ReportManagement {

  def createReportDefinition(record: ReportDefinitionToCreate): Future[ServiceResponse[ReportDefinition]]

  def countReportDefinitionByCriteria(criteriaDto: ReportDefinitionCriteria): Future[ServiceResponse[Int]]

  def getReportDefinitionByCriteria(criteriaDto: ReportDefinitionCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[ReportDefinition]]]

  def getReportDefinitionById(id: UUID): Future[ServiceResponse[ReportDefinition]]

  def deleteReportDefinitionById(id: UUID): Future[ServiceResponse[Boolean]]

  def updateReportDefinition(id: UUID, reportDefinitionToUpdate: ReportDefinitionToUpdate): Future[ServiceResponse[ReportDefinition]]

  def getReportData(id: UUID, queryParams: Map[String, String]): Future[ServiceResponse[Report]]

  def getAvailableReportsForUser(backOfficeUserName: String): Future[ServiceResponse[Seq[ReportDefinitionPermission]]]
}
