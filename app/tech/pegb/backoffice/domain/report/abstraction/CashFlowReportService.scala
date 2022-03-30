package tech.pegb.backoffice.domain.report.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.report.dto.{CashFlowReportCriteria}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.report.implementation
import scala.concurrent.Future

@ImplementedBy(classOf[implementation.CashFlowReportService])
trait CashFlowReportService {

  def getCashFlowReport(
    criteria: CashFlowReportCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[CashFlowReport]]

}
