package tech.pegb.backoffice.mapping.domain.api.report

import java.time.format.DateTimeFormatter

import play.api.libs.json.Json
import tech.pegb.backoffice.api.reportsv2.dto.{ReportDefinitionToRead, ReportResource, CashFlowReport ⇒ ApiCashFlowReport}
import tech.pegb.backoffice.domain.report.abstraction.CashFlowReport
import tech.pegb.backoffice.domain.report.model.ReportDefinition
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.report.dto.ReportDefinitionPermission

object Implicits {

  implicit class ReportDefinitionToReadAdapter(val arg: ReportDefinition) extends AnyVal {
    def asApi: ReportDefinitionToRead = {
      ReportDefinitionToRead(
        id = arg.id,
        name = arg.name,
        title = arg.title,
        description = arg.description,
        columns = arg.columns.getOrElse(Json.arr()),
        parameters = arg.parameters.getOrElse(Json.arr()),
        joins = arg.joins.getOrElse(Json.arr()),
        grouping = arg.grouping.getOrElse(Json.arr()),
        ordering = arg.ordering.getOrElse(Json.arr()),
        paginated = arg.paginated,
        sql = arg.sql,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        updatedBy = arg.updatedBy)
    }
  }

  import tech.pegb.backoffice.util.Implicits._
  implicit class CashFlowReportAdapter(val arg: CashFlowReport) extends AnyVal {
    def asApi(limit: Option[Int], offset: Option[Int]): Seq[ApiCashFlowReport] =
      arg.reportLines
        .drop(offset.getOrElse(0))
        .take(limit.getOrElse(Int.MaxValue))
        .map(c ⇒ ApiCashFlowReport(
          // always print date as "dd/MM/yyyy"
          date = c.date.toZonedDateTimeUTC.toString,
          institution = c.provider + s" (${c.account})",
          currency = c.currency,
          openingBalance = c.openingBalance.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
          bankTransfer = c.bankTransfer.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
          cashIn = c.cashin.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
          cashOut = c.cashout.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
          transactions = c.transactions.setScale(2, BigDecimal.RoundingMode.HALF_EVEN),
          closingBalance = c.closingBalance.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)))
  }

  implicit class ReportResourceAdapter(val arg: ReportDefinitionPermission) extends AnyVal {
    def asApi(baseUrl: String) = ReportResource(
      id = arg.reportDefId,
      name = arg.reportDefName,
      title = arg.reportDefTitle,
      path = s"$baseUrl/${arg.reportDefId}",
      resource = s"$baseUrl/${arg.reportDefId}",
      component = "Report")
  }
}
