package tech.pegb.backoffice.api.reportsv2.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable

@ImplementedBy(classOf[impl.ReportDefinitionController])
trait ReportDefinitionController extends Routable {
  def getRoute: String = "report_definitions"

  def createReportDefinition: Action[JsValue]

  def getReportDefinition(name: Option[String], partialMatch: Option[String],
    orderBy: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent]

  def getReportDefinitionById(id: UUID): Action[AnyContent]

  def updateReportDefinition(id: UUID): Action[JsValue]

  def deleteReportDefinitionById(id: UUID): Action[AnyContent]

}
