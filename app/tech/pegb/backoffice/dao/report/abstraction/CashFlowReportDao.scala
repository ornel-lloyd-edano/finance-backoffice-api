package tech.pegb.backoffice.dao.report.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.report.dto.{CashFlowReportCriteria, CashFlowReportRow, CashFlowTotals, CashFlowTotalsCriteria}
import tech.pegb.backoffice.dao.report.sql.{CashFlowReportGreenPlum}

@ImplementedBy(classOf[CashFlowReportGreenPlum])
trait CashFlowReportDao {

  def getCashFlowReport(criteria: CashFlowReportCriteria): DaoResponse[Seq[CashFlowReportRow]]

}
