package tech.pegb.backoffice.api.reportv2

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import cats.implicits._
import play.api.libs.json.JsValue
import play.api.mvc.Headers
import play.api.test.Helpers.{GET, contentAsString, route}
import play.api.test.{FakeRequest, Injecting}
import play.mvc.Http.HttpVerbs
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.report.abstraction.ReportManagement
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.{PegBNoDbTestApp, PegBTestApp, TestExecutionContext}
import play.api.test.Helpers._
import tech.pegb.backoffice.api.reportsv2.dto.{ReportResource, ReportRoutes}
import tech.pegb.backoffice.domain.report.dto.ReportDefinitionPermission

import scala.concurrent.Future

class ReportResourcesControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  import tech.pegb.backoffice.api.json.Implicits._

  private val httpClient = stub[HttpClient]

  val reportMgmt: ReportManagement = stub[ReportManagement]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[ReportManagement].to(reportMgmt))

  val appConfig = inject[AppConfig]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "ReportResourcesControllerSpec" should {
    "get response of /routes " in {

      val id = UUID.randomUUID()
      implicit val requestId = UUID.randomUUID()

      //val reportPermissions = reportMgmt.getAvailableReportsForUser()
      val expectedReportPermissions = Seq(ReportDefinitionPermission(
        reportDefId = id.toString,
        reportDefName = "Test_Report",
        reportDefTitle = "Test_Title",
        scopeId = "test",
        businessUnitId = "scala",
        roleId = "developer"))

      //val boUser = "tanmoy"

      (reportMgmt.getAvailableReportsForUser(_: String)).when(mockRequestFrom)
        .returns(Future.successful(Right(expectedReportPermissions)))

      val resp = route(app, FakeRequest(GET, s"/routes")
        .withHeaders(jsonHeaders)).get

      val routesArray = Seq(ReportResource(
        id = id.toString,
        name = "Test_Report",
        title = "Test_Title",
        path = s"/reports/$id",
        resource = s"/reports/$id",
        component = "Report"))
      val expectedJson = Seq(ReportRoutes(routes = routesArray)).toJsonStr

      contentAsString(resp) mustBe expectedJson

    }
  }

}
