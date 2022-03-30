package tech.pegb.backoffice.api.reportsv2.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable

@ImplementedBy(classOf[impl.ReportDataController])
trait ReportDataController extends Routable {
  def getRoute: String = "reports"
  def getReportData(id: UUID): Action[AnyContent]
}
