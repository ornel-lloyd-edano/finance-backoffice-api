package tech.pegb.backoffice.api.reportsv2.controllers

import com.google.inject.ImplementedBy
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.reportsv2.controllers.impl.ReportResourcesController

@ImplementedBy(classOf[ReportResourcesController])
trait ReportResourcesControllerT extends Routable {
  def getRoute: String = "routes"
  def getAvailableReportsForUser: Action[AnyContent]
}
