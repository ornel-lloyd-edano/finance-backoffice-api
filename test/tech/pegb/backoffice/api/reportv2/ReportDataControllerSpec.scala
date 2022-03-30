package tech.pegb.backoffice.api.reportv2

import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.domain.report.abstraction.ReportManagement
import tech.pegb.backoffice.domain.report.model.Report
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class ReportDataControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val reportDefinitionManagement = stub[ReportManagement]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[ReportManagement].to(reportDefinitionManagement),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "ReportDataController" should {
    "return report data in GET /reports/{report_definition_id}" in {
      implicit val requestId: UUID = UUID.randomUUID()

      val reportDefinitionId = UUID.randomUUID()
      val param = Map[String, String]()
      val expected = Report(
        count = 2L,
        result = Seq(
          Json.parse("""{"ID":"1","DESCRIPTION":"standard account type for individual users","CREATED_AT":"2019-10-09 14:33:56.994","ACCOUNT_TYPE_NAME":"WALLET","CREATED_BY":null,"UPDATED_AT":null,"IS_ACTIVE":"1","UPDATED_BY":null}"""),
          Json.parse("""{"ID":"2","DESCRIPTION":"standard account type for individual users","CREATED_AT":"2019-10-09 14:33:56.994","ACCOUNT_TYPE_NAME":"WALLET1","CREATED_BY":null,"UPDATED_AT":null,"IS_ACTIVE":"1","UPDATED_BY":null}""")))

      (reportDefinitionManagement.getReportData(_: UUID, _: Map[String, String]))
        .when(reportDefinitionId, param)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(GET, s"/reports/${reportDefinitionId}").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"ID":"1",
           |"DESCRIPTION":"standard account type for individual users",
           |"CREATED_AT":"2019-10-09 14:33:56.994",
           |"ACCOUNT_TYPE_NAME":"WALLET",
           |"CREATED_BY":null,
           |"UPDATED_AT":null,
           |"IS_ACTIVE":"1",
           |"UPDATED_BY":null
           |},
           |{
           |"ID":"2",
           |"DESCRIPTION":"standard account type for individual users",
           |"CREATED_AT":"2019-10-09 14:33:56.994",
           |"ACCOUNT_TYPE_NAME":"WALLET1",
           |"CREATED_BY":null,
           |"UPDATED_AT":null,
           |"IS_ACTIVE":"1",
           |"UPDATED_BY":null
           |}],"limit":null,"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
  }

}
