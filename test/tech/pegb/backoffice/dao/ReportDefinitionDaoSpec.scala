package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import org.scalatest.Matchers._
import tech.pegb.backoffice.dao.auth.abstraction.{PermissionDao, ScopeDao}
import tech.pegb.backoffice.dao.auth.dto.ScopeToInsert
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.dao.report.abstraction.ReportDefinitionDao
import tech.pegb.backoffice.dao.report.dto.{ReportDefinitionCriteria, ReportDefinitionToInsert, ReportDefinitionToUpdate}
import tech.pegb.backoffice.dao.report.entity.ReportDefinition
import tech.pegb.core.PegBTestApp

class ReportDefinitionDaoSpec extends PegBTestApp {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[ReportDefinitionDao]
  private val scopeDao = inject[ScopeDao]
  private val permissionDao = inject[PermissionDao]
  override def initSql =
    s"""
       |INSERT INTO `scopes` (id,parentId,`name`,description,is_active,created_by,updated_by,created_at,updated_at)
       | VALUES
       |('123deca2-be7e-11e9-973e-abcc297e335a', null , 'reporting', 'Test Report Parent', 1,
       |'tanmoy','tanmoy',now(),now()),
       |
       |('d40c2ce7-be7e-11e9-973e-000c297e3e45', '123deca2-be7e-11e9-973e-abcc297e335a' , 'test_report_0', 'Test Report 2', 1,
       |'tanmoy','tanmoy',now(),now()),
       |
       |('abcdece7-be7e-11e9-973e-000c297e3e45', '123deca2-be7e-11e9-973e-abcc297e335a' , 'test_report_1', 'Test Report 1', 1,
       |'tanmoy','tanmoy',now(),now());
       |
       |
       |INSERT INTO `report_definitions`
       |(id, report_name, report_title, report_description, report_columns, parameters, joins, grouping_columns, ordering, raw_sql, created_at, created_by, updated_at, updated_by, paginated)
       |VALUES('0f0b9fa5-55f6-423f-8727-bc929268ba08','test_report_1', 'Test Report', 'Generic Transactions report',
       |'[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"74","operation":"sum"},{"name":"amount","title":"Amount","source":"transactions","type":87}]',
       |'[{"name":"type","title":"Type","type":"text","required":true},{"name":"limit","title":"Page size","type":"number","required":true},{"name":"offset","title":"Page","type":"number","required":true}]',
       |null,
       |'[{"expression":"string"}]',
       |'[{"name":"string","descending":true}]',
       |'select * from pegb_wallet_dwh.transactions where type = {type} or {type} = '''' limit {limit} offset {offset}',
       |now(), 'tanmoy',now(),'tanmoy', false),
       |
       |('aaaa9fa5-55f6-423f-8727-bc929268ba08','test_report_0', 'Test Report 2', 'This is Generic Transactions report',
       |'[{"name":"primaryAccountId","title":"Primary account","source":"transactions","type":"74","operation":"sum"},{"name":"amount","title":"Amount","source":"transactions","type":87}]',
       |'[{"name":"type","title":"Type","type":"text","required":true},{"name":"limit","title":"Page size","type":"number","required":true},{"name":"offset","title":"Page","type":"number","required":true}]',
       |null,
       |'[{"expression":"string"}]',
       |'[{"name":"string","descending":true}]',
       |'select * from pegb_wallet_dwh.transactions where type = {type} or {type} = '''' limit {limit} offset {offset}'
       |,now(), 'tanmoy',now(),'tanmoy', false
       |);
       |
       |
       |INSERT INTO roles (id,`name`,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('047ddb43-3bc8-4b4d-ae62-822c2a08e49a','super_admin',1,'system','system',now(),now());
       |
       |INSERT INTO business_units (id,`name`,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('85bc6f6e-81a3-4e9a-a282-da649cfb427c','super-admin',1,'system','system',now(),now());
       |
       |INSERT INTO back_office_users (id,userName,password,roleId,businessUnitId,email,phoneNumber,firstName,middleName,lastName,description,homePage,is_active,activeLanguage,customData,lastLoginTimestamp,created_at,updated_at,created_by,updated_by) VALUES
       |('6ebfa18c-d21d-11e8-bcd3-000c291e73b1','superadmin','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B',
       |'047ddb43-3bc8-4b4d-ae62-822c2a08e49a','85bc6f6e-81a3-4e9a-a282-da649cfb427c','superadmin@pegb.tech','97123456789','super',NULL,'admin','super description test','https://pegb.tech',1,NULL,NULL,1546940581250,now(),now(),'system','system');
       |
       |
       |INSERT INTO permissions (id,buId,userId,roleId,scopeId,canWrite,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('c4b8b9ac-af50-4f38-8bc6-1e051d042735',NULL,'6ebfa18c-d21d-11e8-bcd3-000c291e73b1',NULL,'abcdece7-be7e-11e9-973e-000c297e3e45',1,1,'system','system',now(),now()),
       |('a7b2c9ac-af50-4f38-8bc6-2a050d02b715','85bc6f6e-81a3-4e9a-a282-da649cfb427c',NULL,'047ddb43-3bc8-4b4d-ae62-822c2a08e49a','abcdece7-be7e-11e9-973e-000c297e3e45',1,1,'system','system',now(),now());
       |
    """.stripMargin

  "Permission DAO" should {
    "get permission id by scope id" in {
      val result = permissionDao.getPermissionIdsByScopeId("abcdece7-be7e-11e9-973e-000c297e3e45")

      result mustBe Right(Seq("c4b8b9ac-af50-4f38-8bc6-1e051d042735", "a7b2c9ac-af50-4f38-8bc6-2a050d02b715"))
    }
  }

  "Scope DAO" should {
    "get scope id by scope name" in {
      val result = scopeDao.getScopeIdByName("test_report_1")

      result mustBe Right("abcdece7-be7e-11e9-973e-000c297e3e45".some)
    }
  }

  "Report Definition Dao" should {
    "return all report definition on getAll" in {
      val orderingSet = OrderingSet(Ordering("report_name", Ordering.ASC)).some
      val criteria = ReportDefinitionCriteria()
      val res = dao.getReportDefinitionByCriteria(criteria, orderingSet, None, None)

      res.right.get.map(_.name) mustBe Seq("test_report_0", "test_report_1")
    }

    "return matching report definition by id" in {
      val orderingSet = None
      val criteria = ReportDefinitionCriteria(id = CriteriaField("", "aaaa9fa5", MatchTypes.Partial).some)
      val res = dao.getReportDefinitionByCriteria(criteria, orderingSet, None, None)

      res.right.get.map(_.name) mustBe Seq("test_report_0")
    }

    "return matching report definition by name" in {
      val orderingSet = None
      val criteria = ReportDefinitionCriteria(name = "test_report_1".some)
      val res = dao.getReportDefinitionByCriteria(criteria, orderingSet, None, None)

      res.right.get.map(_.name) mustBe Seq("test_report_1")
    }

    "return matching report definition by title" in {
      val orderingSet = None
      val criteria = ReportDefinitionCriteria(title = "Test Report".some)
      val res = dao.getReportDefinitionByCriteria(criteria, orderingSet, None, None)

      res.right.get.map(_.name) mustBe Seq("test_report_1")
    }

    "return matching report definition by description" in {
      val orderingSet = None
      val criteria = ReportDefinitionCriteria(description = CriteriaField("", "This is", MatchTypes.Partial).some)
      val res = dao.getReportDefinitionByCriteria(criteria, orderingSet, None, None)

      res.right.get.map(_.name) mustBe Seq("test_report_0")
    }

    "return report definition on create " in {
      val scope = ScopeToInsert(
        parentId = "123deca2-be7e-11e9-973e-abcc297e335a".some,
        name = "new_report_definition",
        description = "Generic Transaction's for report".some,
        isActive = 1,
        createdBy = "pegbuser",
        createdAt = now)

      val reportDto = ReportDefinitionToInsert(
        name = scope.name,
        title = "Test single quotes in raw sql",
        description = "Generic Transaction's for report",
        columns = "[{\"name\":\"primaryAccountId\",\"title\":\"Primary account\",\"source\":\"transactions\",\"type\":\"74\",\"operation\":\"sum\"},{\"name\":\"amount\",\"title\":\"Amount\",\"source\":\"transactions\",\"type\":87}]".some,
        parameters = "[{\"name\":\"type\",\"title\":\"Type\",\"type\":\"text\",\"required\":true},{\"name\":\"limit\",\"title\":\"Page size\",\"type\":\"number\",\"required\":true},{\"name\":\"offset\",\"title\":\"Page\",\"type\":\"number\",\"required\":true}]".some,
        joins = None,
        grouping = "[{\"expression\":\"string\"}]".some,
        ordering = "[{\"name\":\"string\",\"descending\":true}]".some,
        paginated = false,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        createdBy = "pegbuser",
        createdAt = now)
      val res = dao.createReportDefinition(reportDto, scope)

      val actualResult = res.right.get
      actualResult.name mustBe reportDto.name
      actualResult.title mustBe reportDto.title
      actualResult.description mustBe reportDto.description
      actualResult.columns mustBe reportDto.columns
      actualResult.parameters mustBe reportDto.parameters
      actualResult.joins mustBe reportDto.joins
      actualResult.grouping mustBe reportDto.grouping
      actualResult.ordering mustBe reportDto.ordering
      actualResult.paginated mustBe reportDto.paginated
      actualResult.sql mustBe reportDto.sql
      actualResult.createdAt mustBe reportDto.createdAt
      actualResult.createdBy mustBe reportDto.createdBy
      actualResult.updatedAt mustBe reportDto.createdAt.some
      actualResult.updatedBy mustBe reportDto.createdBy.some
    }

    "return updated report definition on update " in {
      val reportDto = ReportDefinitionToUpdate(
        title = "Test update",
        description = "Generic Transaction's for report",
        columns = "[{\"name\":\"primaryAccountId\",\"title\":\"Primary account\",\"source\":\"transactions\",\"type\":\"74\",\"operation\":\"sum\"},{\"name\":\"amount\",\"title\":\"Amount\",\"source\":\"transactions\",\"type\":87}]".some,
        parameters = "[{\"name\":\"type\",\"title\":\"Type\",\"type\":\"text\",\"required\":true},{\"name\":\"limit\",\"title\":\"Page size\",\"type\":\"number\",\"required\":true},{\"name\":\"offset\",\"title\":\"Page\",\"type\":\"number\",\"required\":true}]".some,
        joins = None,
        grouping = "[{\"expression\":\"string\"}]".some,
        ordering = "[{\"name\":\"string\",\"descending\":true}]".some,
        paginated = false,
        sql = "select * from pegb_wallet_dwh.transactions where type = ''",
        updatedBy = "pegbuser",
        updatedAt = now)

      val res = dao.updateReportDefinitionById("aaaa9fa5-55f6-423f-8727-bc929268ba08", reportDto)

      res.right.get should not be none[ReportDefinition]
      val actualResult = res.right.get.get
      actualResult.name mustBe "test_report_0"
      actualResult.title mustBe reportDto.title
      actualResult.description mustBe reportDto.description
      actualResult.columns mustBe reportDto.columns
      actualResult.parameters mustBe reportDto.parameters
      actualResult.joins mustBe reportDto.joins
      actualResult.grouping mustBe reportDto.grouping
      actualResult.ordering mustBe reportDto.ordering
      actualResult.paginated mustBe reportDto.paginated
      actualResult.sql mustBe reportDto.sql
    }

    "get report definitions available for backoffice user" in {
      val mockBackOfficeUserName = "superadmin"

      val result = dao.getReportDefinitionPermissionByBackOfficeUserName(mockBackOfficeUserName)
      val expected = Seq(("0f0b9fa5-55f6-423f-8727-bc929268ba08", "test_report_1", "Test Report"))
      result.isRight mustBe true
      result.right.get.map(p ⇒ (p.reportDefId, p.reportDefName, p.reportDefTitle)) mustBe expected
    }

    "delete report definition + scope and permission if defined" in {
      val res = dao.deleteReportDefinitionById("0f0b9fa5-55f6-423f-8727-bc929268ba08", "abcdece7-be7e-11e9-973e-000c297e3e45".some, Seq("a7b2c9ac-af50-4f38-8bc6-2a050d02b715", "c4b8b9ac-af50-4f38-8bc6-1e051d042735"))
      val report = dao.getReportDefinitionById("0f0b9fa5-55f6-423f-8727-bc929268ba08")
      val scope = scopeDao.getScopeIdByName("test_report_1")
      val permission = permissionDao.getPermissionIdsByScopeId("abcdece7-be7e-11e9-973e-000c297e3e45")

      res mustBe Right(true)
      report mustBe Right(None)
      scope mustBe Right(None)
      permission mustBe Right(Nil)
    }
  }

}
