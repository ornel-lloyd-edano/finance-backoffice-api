package tech.pegb.backoffice.domain.report.implementation
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.report.abstraction.{CashFlowReportDao, ReportDefinitionDao}
import tech.pegb.backoffice.dao.report.dto.ReportDefinitionCriteria
import tech.pegb.backoffice.domain.{BaseService, model}
import tech.pegb.backoffice.domain.report.abstraction
import tech.pegb.backoffice.domain.report.abstraction.{CashFlowReport, CustomReportDefinition}
import tech.pegb.backoffice.domain.report.dto.CashFlowReportCriteria
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.report.cashflow.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.report.cashflow.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class CashFlowReportService @Inject() (
    appConfig: AppConfig,
    executionContexts: WithExecutionContexts,
    reportDefDao: ReportDefinitionDao,
    cashFlowReportDao: CashFlowReportDao) extends CustomReportDefinition with abstraction.CashFlowReportService with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  implicit val dateRangeConfig = appConfig.DateTimeRangeLimits.dateTimeRangeConfig.some

  val id = appConfig.Reports.cashflowReportUuid
  val name = "cashflow"
  val title = "Cash Flow Report"
  val description = "reports the opening/closing balance of an provider or institution" +
    " as well as total cashins, cashouts and other transactions"

  assert(isUniqueReport, s"Custom report [$name] conflicts an existing report in the report definitions.")

  def isUniqueReport: Boolean = {
    val isUniqueId = reportDefDao.getReportDefinitionById(id.toString).bimap(_ ⇒ false, _.isEmpty).merge
    val isUniqueName = reportDefDao.getReportDefinitionByCriteria(
      criteria = ReportDefinitionCriteria(name = Some(this.name)), None, None, None)
      .bimap(_ ⇒ false, _.isEmpty).merge

    isUniqueId && isUniqueName
  }

  def getCashFlowReport(
    criteria: CashFlowReportCriteria,
    orderBy: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[CashFlowReport]] = Future {

    for {
      _ ← criteria.validateDateRange.leftMap(error ⇒ validationError(error.getMessage))
      result ← cashFlowReportDao.getCashFlowReport(criteria.asDao)
        .bimap(_.asDomainError, _.asDomain)
    } yield {
      result
    }
  }

}
