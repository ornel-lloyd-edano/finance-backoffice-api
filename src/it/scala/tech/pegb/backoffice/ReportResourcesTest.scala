package tech.pegb.backoffice

import play.api.db.{DBApi, Database}
import play.api.test._
import play.api.test.Helpers._
import tech.pegb.backoffice.api.reportsv2.controllers.ReportResourcesControllerT
import tech.pegb.backoffice.dao.auth.abstraction.{BackOfficeUserDao, PermissionDao, ScopeDao}
import tech.pegb.backoffice.dao.auth.dto.{PermissionToInsert, ScopeToInsert}
import tech.pegb.backoffice.dao.auth.entity.BackOfficeUser
import tech.pegb.backoffice.domain.auth.dto.BackOfficeUserCriteria
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._

class ReportResourcesTest extends PlayIntegrationTest {

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
       |now(), 'tanmoy',now(),'tanmoy', false);
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
       |,now(), 'tanmoy',now(),'tanmoy', false
       |);
     """.stripMargin

  override val endpoint = s"/api/${inject[ReportResourcesControllerT].getRoute}"

  "Report Resources api" should {
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

      val permissionDao = inject[PermissionDao]
      val backOfficeUserDao = inject[BackOfficeUserDao]

      val superuser: BackOfficeUser = backOfficeUserDao.getBackOfficeUsersByCriteria(
        Some(BackOfficeUserCriteria(userName = Some("superuser")).asDao()),
        None, None, None).right.get.head

      permissionDao.insertPermission(PermissionToInsert(
        businessUnitId = Some(superuser.businessUnitId),
        roleId = Some(superuser.roleId),
        userId = None,
        canWrite = Some(1),
        isActive = Some(1),
        scopeId = scope1.id,
        createdAt = now,
        createdBy = "integration_test"
      ))

      permissionDao.insertPermission(PermissionToInsert(
        businessUnitId = Some(superuser.businessUnitId),
        roleId = Some(superuser.roleId),
        userId = None,
        canWrite = Some(1),
        isActive = Some(1),
        scopeId = scope2.id,
        createdAt = now,
        createdBy = "integration_test"
      ))

    }

    "get available reports for backoffice user" in {
      val request = FakeRequest("GET", s"$endpoint").withHeaders(AuthHeader)
      val resp = route(app, request).get

      status(resp) mustBe OK

      println(resp)
      val expected =
        s"""
           |[{"name":"reporting","title":"reporting","key":"reporting","routes":
           |[{"id":"0f0b9fa5-55f6-423f-8727-bc929268ba08","name":"test_report_1","title":"Test Report","path":"/reports/0f0b9fa5-55f6-423f-8727-bc929268ba08","resource":"/reports/0f0b9fa5-55f6-423f-8727-bc929268ba08","component":"Report"},
           |{"id":"aaaa9fa5-55f6-423f-8727-bc929268ba08","name":"test_report_2","title":"Test Report 2","path":"/reports/aaaa9fa5-55f6-423f-8727-bc929268ba08","resource":"/reports/aaaa9fa5-55f6-423f-8727-bc929268ba08","component":"Report"}
           |]}]
         """.stripMargin.trim.replace(System.lineSeparator(),"")

      //"[{"[[[name":"reporting","title":"reporting","key":"reporting","routes":[{"id":"0f0b9fa5-55f6-423f-8727-bc929268ba08","name":"test_report_1","title":"Test Report","path":"/reports/0f0b9fa5-55f6-423f-8727-bc929268ba08","resource":"/reports/0f0b9fa5-55f6-423f-8727-bc929268ba08","component":"Report"},{"id":"aaaa9fa5-55f6-423f-8727-bc929268ba08","name":"test_report_2","title":"Test Report 2","path":"/reports/aaaa9fa5-55f6-423f-8727-bc929268ba08","resource":"/reports/aaaa9fa5-55f6-423f-8727-bc929268ba08","component":"Report"}]]]]}]"

      contentAsString(resp) mustBe expected
    }
  }

}
