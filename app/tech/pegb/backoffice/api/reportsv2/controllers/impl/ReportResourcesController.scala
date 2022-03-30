package tech.pegb.backoffice.api.reportsv2.controllers.impl

import java.util.UUID

import cats.implicits._
import com.google.inject.{Inject, Singleton}
import io.swagger.annotations.Api
import play.api.db.DBApi
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.report.Implicits._
import tech.pegb.backoffice.api.reportsv2.controllers.ReportResourcesControllerT
import tech.pegb.backoffice.api.reportsv2.dto.ReportRoutes
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.domain.report.abstraction.ReportManagement
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
@Api(value = "Report Resources", produces = "application/json", consumes = "application/json")
class ReportResourcesController @Inject() (
    executionContexts: WithExecutionContexts,
    reportMgmtService: ReportManagement,
    implicit val appConfig: AppConfig,
    val dbApi: DBApi,
    controllerComponents: ControllerComponents) extends ApiController(controllerComponents) with RequiredHeaders with ReportResourcesControllerT {

  implicit val ec = executionContexts.genericOperations

  def getAvailableReportsForUser: Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val loggedInUser = getRequestFrom
    val tmp = reportMgmtService.getAvailableReportsForUser(loggedInUser)
      .map(_.bimap(_.asApiError(), _.map(_.asApi("/reports")))
        // TODO Frontend is expecting an array of single-element. Hence, using the Seq[ReportRoutes] to satisfy this condition
        .map(routesArray ⇒ Seq(ReportRoutes(routes = routesArray)).toJsonStr))
      .map(handleApiResponse(_))
    tmp
  }

}
