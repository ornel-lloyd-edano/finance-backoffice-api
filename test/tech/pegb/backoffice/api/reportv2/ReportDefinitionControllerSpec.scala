package tech.pegb.backoffice.api.reportv2

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.report.abstraction.ReportManagement
import tech.pegb.backoffice.domain.report.dto
import tech.pegb.backoffice.domain.report.dto.{ReportDefinitionCriteria, ReportDefinitionToCreate, ReportDefinitionToUpdate}
import tech.pegb.backoffice.domain.report.model.ReportDefinition
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class ReportDefinitionControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val reportDefinitionManagement = stub[ReportManagement]
  private val latestVersion = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[ReportManagement].to(reportDefinitionManagement),
      bind[LatestVersionService].to(latestVersion),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "ReportDefinitionController" should {
    "create reportDefinition and respond with ReportDefinitionToRead json in POST /report_definitions" in {
      implicit val requestId: UUID = UUID.randomUUID()

      val jsonRequest =
        s"""{
          "name": "test_single_quotes",
          "title": "Test single quotes in raw sql",
          "description": "Generic Transaction's for report",
          "joins": null,
          "columns": [
            {
              "name": "primaryAccountId",
              "title": "Primary account",
              "source": "transactions",
              "type": "int",
              "operation": "sum"
            }
          ],
          "parameters": [
            {
              "name": "type",
              "title": "Type",
              "type": "text",
              "required": true,
              "comparator": "equal",
              "options": []
            }
          ],
          "grouping": [],
          "ordering": [
            {
              "name": "primaryAccountId",
              "descending": true
            }
          ],
          "paginated":true,
          "sql": "select * from pegb_wallet_dwh.transactions where type = ''"
        }"""

      val domainDto = ReportDefinitionToCreate(
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = ReportDefinition(
        id = UUID.randomUUID(),
        name = domainDto.name,
        title = domainDto.title,
        description = domainDto.description,
        columns = domainDto.columns,
        parameters = domainDto.parameters,
        joins = domainDto.joins,
        grouping = domainDto.grouping,
        ordering = domainDto.ordering,
        paginated = domainDto.paginated,
        sql = domainDto.sql,
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expectedJson =
        s"""{
           |"id":"${expected.id}",
           |"name":"test_single_quotes",
           |"title":"Test single quotes in raw sql",
           |"description":"Generic Transaction's for report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}],
           |"joins":[],
           |"grouping":[],
           |"ordering":[{"name":"primaryAccountId","descending":true}],
           |"paginated":true,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = ''",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (reportDefinitionManagement.createReportDefinition(_: ReportDefinitionToCreate)).when(domainDto)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(POST, s"/report_definitions", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "respond with ReportDefinitionToRead json in GET /report/definition/id" in {
      val expected = ReportDefinition(
        id = UUID.randomUUID(),
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expectedJson =
        s"""{
           |"id":"${expected.id}",
           |"name":"test_single_quotes",
           |"title":"Test single quotes in raw sql",
           |"description":"Generic Transaction's for report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}],
           |"joins":[],
           |"grouping":[],
           |"ordering":[{"name":"primaryAccountId","descending":true}],
           |"paginated":true,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = ''",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (reportDefinitionManagement.getReportDefinitionById(_: UUID)).when(expected.id)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(GET, s"/report_definitions/${expected.id}").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }

    "update reportDefinition and respond with ReportDefinitionToRead json in PUT /report/definition" in {
      implicit val requestId: UUID = UUID.randomUUID()

      val jsonRequest =
        s"""{
          "title": "Test single quotes in raw sql",
          "description": "Generic Transaction's for report",
          "joins": null,
          "columns": [
            {
              "name": "primaryAccountId",
              "title": "Primary account",
              "source": "transactions",
              "type": "int",
              "operation": "sum"
            }
          ],
          "parameters": [
            {
              "name": "type",
              "title": "Type",
              "type": "text",
              "required": true,
              "comparator": "equal",
              "options": []
            }
          ],
          "grouping": [],
          "ordering": [
            {
              "name": "primaryAccountId",
              "descending": true
            }
          ],
          "paginated":true,
          "sql": "select * from pegb_wallet_dwh.transactions where type = ''"
        }"""

      val domainDto = ReportDefinitionToUpdate(
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        updatedBy = "pegbuser",
        updatedAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = ReportDefinition(
        id = UUID.randomUUID(),
        name = "test_single_quotes",
        title = domainDto.title,
        description = domainDto.description,
        columns = domainDto.columns,
        parameters = domainDto.parameters,
        joins = domainDto.joins,
        grouping = domainDto.grouping,
        ordering = domainDto.ordering,
        paginated = domainDto.paginated,
        sql = domainDto.sql,
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expectedJson =
        s"""{
           |"id":"${expected.id}",
           |"name":"test_single_quotes",
           |"title":"Test single quotes in raw sql",
           |"description":"Generic Transaction's for report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}],
           |"joins":[],
           |"grouping":[],
           |"ordering":[{"name":"primaryAccountId","descending":true}],
           |"paginated":true,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = ''",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (reportDefinitionManagement.updateReportDefinition(_: UUID, _: dto.ReportDefinitionToUpdate))
        .when(expected.id, domainDto)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(PUT, s"/report_definitions/${expected.id}", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "respond with ReportDefinitionToRead json in GET /report/definition" in {
      val rd1 = ReportDefinition(
        id = UUID.randomUUID(),
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)
      val rd2 = ReportDefinition(
        id = UUID.randomUUID(),
        name = "test_single_quotes",
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = Json.parse("""[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}]""").as[JsArray].some,
        parameters = Json.parse("""[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}]""").as[JsArray].some,
        joins = None,
        grouping = Json.parse("[]").as[JsArray].some,
        ordering = Json.parse("""[{"name":"primaryAccountId","descending":true}]""").as[JsArray].some,
        paginated = true,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = "pegbuser".some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expected = Seq(rd1, rd2)

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${rd1.id}",
           |"name":"test_single_quotes",
           |"title":"Test single quotes in raw sql",
           |"description":"Generic Transaction's for report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}],
           |"joins":[],
           |"grouping":[],
           |"ordering":[{"name":"primaryAccountId","descending":true}],
           |"paginated":true,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = ''",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser"
           |},
           |{
           |"id":"${rd2.id}",
           |"name":"test_single_quotes",
           |"title":"Test single quotes in raw sql",
           |"description":"Generic Transaction's for report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"int","operation":"sum"}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true,"comparator":"equal","options":[]}],
           |"joins":[],
           |"grouping":[],
           |"ordering":[{"name":"primaryAccountId","descending":true}],
           |"paginated":true,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = ''",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser"
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val mockLatestVersion = LocalDateTime.now.toString
      val reportDefinitionCriteria = ReportDefinitionCriteria(
        name = "test_single_quotes".some,
        partialMatchFields = Set("id", "description"))

      (latestVersion.getLatestVersion _).when(reportDefinitionCriteria).returns(Future.successful(Right(mockLatestVersion.some)))

      (reportDefinitionManagement.countReportDefinitionByCriteria _).when(reportDefinitionCriteria)
        .returns(Future.successful(Right(2)))
      import tech.pegb.backoffice.domain.model.Ordering
      (reportDefinitionManagement.getReportDefinitionByCriteria _)
        .when(reportDefinitionCriteria, Seq(Ordering("report_name", Ordering.ASCENDING), Ordering("created_at", Ordering.DESCENDING)), None, None)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(GET, s"/report_definitions?name=test_single_quotes&order_by=name,-created_at").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }
  }
}
