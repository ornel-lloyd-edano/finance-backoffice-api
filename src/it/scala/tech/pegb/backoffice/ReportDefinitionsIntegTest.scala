package tech.pegb.backoffice

import anorm.{Row, SQL}
import play.api.db.{DBApi, Database}
import play.api.test.Helpers._
import play.api.test._
import tech.pegb.backoffice.api.reportsv2.controllers.{ReportDefinitionController ⇒ ReportDefinitionControllerT}
import tech.pegb.backoffice.dao.auth.abstraction.{ScopeDao}
import tech.pegb.backoffice.dao.auth.dto.{ScopeToInsert}

class ReportDefinitionsIntegTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  override def cleanupSql: String =
    s"""
       |DELETE FROM permissions;
       |DELETE FROM back_office_users;
       |DELETE FROM roles;
       |DELETE FROM business_units;
       |DELETE FROM report_definitions;
       |DELETE FROM scopes;
     """.stripMargin

  override def initSql: String =
    s"""
       |INSERT INTO `report_definitions`
       |(id, report_name, report_title, report_description, report_columns, parameters, joins, grouping_columns, ordering, raw_sql, created_at, created_by, updated_at, updated_by, paginated)
       |VALUES('0f0b9fa5-55f6-423f-8727-bc929268ba08','test_report_1', 'Test Report', 'Generic Transactions report',
       |'[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"74","operation":"sum"},{"name":"amount","title":"Amount","source":"transactions","type":87}]',
       |'[{"name":"type","title":"Type","type":"text","required":true},{"name":"limit","title":"Page size","type":"number","required":true},{"name":"offset","title":"Page","type":"number","required":true}]',
       |null,
       |'[{"expression":"string"}]',
       |'[{"name":"string","descending":true}]',
       |'select * from pegb_wallet_dwh.transactions where type = {type} or {type} = '''' limit {limit} offset {offset}',
       |'2019-01-01 00:00:00', 'tanmoy','2019-01-01 00:00:00','tanmoy', false);
       |
       |
       |INSERT INTO `report_definitions`
       |(id, report_name, report_title, report_description, report_columns, parameters, joins, grouping_columns, ordering, raw_sql, created_at, created_by, updated_at, updated_by, paginated)
       |VALUES
       |('aaaa9fa5-55f6-423f-8727-bc929268ba08','test_report_2', 'Test Report 2', 'This is Generic Transactions report',
       |'[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"74","operation":"sum"},{"name":"amount","title":"Amount","source":"transactions","type":87}]',
       |'[{"name":"type","title":"Type","type":"text","required":true},{"name":"limit","title":"Page size","type":"number","required":true},{"name":"offset","title":"Page","type":"number","required":true}]',
       |null,
       |'[{"expression":"string"}]',
       |'[{"name":"string","descending":true}]',
       |'select * from pegb_wallet_dwh.transactions where type = {type} or {type} = '''' limit {limit} offset {offset}'
       |,'2019-01-01 00:00:00', 'tanmoy','2019-01-01 00:00:00','tanmoy', false
       |);
     """.stripMargin

  override val endpoint = s"/api/${inject[ReportDefinitionControllerT].getRoute}"

  "Report Definitions api" should {
    val db: Database = inject[DBApi].database("backoffice")

    "[preconditions] make sure the scopes related to the report definitions are inserted and linked to permissions for superuser" in {
      val scopeDao = inject[ScopeDao]

      val parentId = scopeDao.getScopeIdByName("reporting").right.get

      val scope1 = scopeDao.insertScope(
        ScopeToInsert(
          name = "test_report_1",
          parentId = parentId,
          description = None,
          isActive = 1,
          createdBy = "integration_test",
          createdAt = now
        )).right.get

      val scope2 = scopeDao.insertScope(
        ScopeToInsert(
          name = "test_report_2",
          parentId = parentId,
          description = None,
          isActive = 1,
          createdBy = "integration_test",
          createdAt = now
        )).right.get
    }

    "get all report definitions" in {
      val request = FakeRequest("GET", s"$endpoint?order_by=name").withHeaders(AuthHeader)
      val resp = route(app, request).get

      status(resp) mustBe OK
      val expected =
        s"""
           |{"total":2,
           |"results":[
           |{"id":"0f0b9fa5-55f6-423f-8727-bc929268ba08",
           |"name":"test_report_1",
           |"title":"Test Report",
           |"description":"Generic Transactions report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"74","operation":"sum"},
           |{"name":"amount","title":"Amount","source":"transactions","type":87}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true},
           |{"name":"limit","title":"Page size","type":"number","required":true},
           |{"name":"offset","title":"Page","type":"number","required":true}],
           |"joins":[],
           |"grouping":[{"expression":"string"}],
           |"ordering":[{"name":"string","descending":true}],
           |"paginated":false,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = {type} or {type} = '' limit {limit} offset {offset}",
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"tanmoy",
           |"updated_at":"2019-01-01T00:00:00Z",
           |"updated_by":"tanmoy"},
           |{"id":"aaaa9fa5-55f6-423f-8727-bc929268ba08",
           |"name":"test_report_2",
           |"title":"Test Report 2",
           |"description":"This is Generic Transactions report",
           |"columns":[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"74","operation":"sum"},
           |{"name":"amount","title":"Amount","source":"transactions","type":87}],
           |"parameters":[{"name":"type","title":"Type","type":"text","required":true},
           |{"name":"limit","title":"Page size","type":"number","required":true},
           |{"name":"offset","title":"Page","type":"number","required":true}],
           |"joins":[],
           |"grouping":[{"expression":"string"}],
           |"ordering":[{"name":"string","descending":true}],
           |"paginated":false,
           |"sql":"select * from pegb_wallet_dwh.transactions where type = {type} or {type} = '' limit {limit} offset {offset}",
           |"created_at":"2019-01-01T00:00:00Z",
           |"created_by":"tanmoy",
           |"updated_at":"2019-01-01T00:00:00Z",
           |"updated_by":"tanmoy"}],"limit":null,"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(),"")

      contentAsString(resp) mustBe expected
    }

    "create report definition" in {
      val jsonPayload =
        s"""{
          "name": "test_report_definition",
          "title": "Test Report",
          "description": "Generic Transactions Report",
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

      val request = FakeRequest("POST", s"$endpoint").withBody(jsonPayload)
        .withHeaders(AuthHeader).withHeaders(jsonHeaders)

      val resp = route(app, request).get

      status(resp) mustBe CREATED

      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM report_definitions WHERE report_name = 'test_report_definition'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isReallyInDB mustBe true

      val isCorrespondingScopeInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM scopes WHERE name = 'test_report_definition'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isCorrespondingScopeInDB mustBe true
    }

    "update report definition" in {
      val jsonPayload =
        s"""{
          "title": "Updated Test Report",
          "description": "Updated Generic Transactions Report",
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

      val id = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT id FROM report_definitions WHERE report_name = 'test_report_definition'")
        result.executeQuery().as(result.defaultParser.singleOpt).map(r⇒ r[String]("id")).get
      }

      val request = FakeRequest("PUT", s"$endpoint/$id").withBody(jsonPayload)
        .withHeaders(AuthHeader).withHeaders(jsonHeaders)

      val resp = route(app, request).get

      status(resp) mustBe OK

      val (updatedTitleInDB, updatedDescInDB) = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM report_definitions WHERE report_name = 'test_report_definition'")
        result.executeQuery().as(result.defaultParser.singleOpt)
          .map(r⇒ (r[String]("report_title"), r[String]("report_description"))).get
      }
      updatedTitleInDB mustBe "Updated Test Report"
      updatedDescInDB mustBe "Updated Generic Transactions Report"
    }

    "delete report definitions" in {
      val id = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT id FROM report_definitions WHERE report_name = 'test_report_definition'")
        result.executeQuery().as(result.defaultParser.singleOpt).map(r⇒ r[String]("id")).get
      }
      val request = FakeRequest("DELETE", s"$endpoint/$id").withHeaders(AuthHeader)
      val resp = route(app, request).get

      status(resp) mustBe OK

      val isReportDefStillInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM report_definitions WHERE report_name = 'test_report_definition'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isReportDefStillInDB mustBe false

      val isRelatedScopeStillInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM scopes WHERE name = 'test_report_definition'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isRelatedScopeStillInDB mustBe false
    }

  }

}
