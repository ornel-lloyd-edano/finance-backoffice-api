package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import org.scalatest.PrivateMethodTester
import play.api.db.DBApi
import play.api.libs.json.{JsValue, Json}
import tech.pegb.backoffice.dao.report.abstraction.ReportDao
import tech.pegb.core.PegBTestApp

class ReportDaoSpec extends PegBTestApp with PrivateMethodTester {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  lazy val dao = fakeApplication().injector.instanceOf[ReportDao]

  override def initDb(): Unit = {
    val dbApi = inject[DBApi]
    val db = dbApi.database("reports")
    val connection = db.getConnection()
    connection.prepareStatement(initSql).executeUpdate()
    connection.commit()
  }

  //FIXME evolutions should be performed for 'reports' data source too
  override def initSql =
    s"""
       |SET SCHEMA $reportsSchema;
       |
       |CREATE TABLE `account_types` (
          `id` int(3) unsigned NOT NULL AUTO_INCREMENT,
          `account_type_name` varchar(15) NOT NULL,
          `description` varchar(100) DEFAULT NULL,
          `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
          `created_by` varchar(36) NULL,
          `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `updated_by` varchar(36) DEFAULT NULL,
          `is_active` int(4) NOT NULL DEFAULT '1',
          PRIMARY KEY (`id`)
        );
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('2', 'WALLET1', 'standard account type for individual users', now(), null, 1);
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('3', 'WALLET3', 'standard account type for individual users', now(), null, 1);
      """.stripMargin

  override def cleanupSql =
    "DELETE FROM account_types;DROP TABLE account_types;"

  "ReportDao executeSql" should {
    "execute a query by replacing its parameters by name first, outstanding params are ignored" in {
      val sqlQuery = "select * from account_types where account_type_name = {p} and created_at >= {b} and created_at <= {b1} and is_active = {c}"
      val params = Map(
        "b1" → LocalDateTime.now().plusSeconds(10).toString(),
        "p" → "WALLET1",
        "n" → "2",
        "c" → "1")

      val paramsJsArray = Json.arr(
        Json.obj("name" → "b", "type" → "datetime", "default" → "2019-01-01"),
        Json.obj("name" → "b1", "type" → "datetime"),
        Json.obj("name" → "p", "type" → "text"),
        Json.obj("name" → "c", "type" → "boolean"))
      val result = dao.executeRawSql(sqlQuery, paramsJsArray, params)

      result.right.get.count mustBe 1

    }

    // Test the limit and offset logic
    "add limit offset to original sql if query has limit and offset in query params" in {
      val sqlQuery = "select * from account_types where is_active = {c} or '{c}' = ''"
      val params = Map(
        "c" → "1",
        "limit" → "2",
        "offset" → "0")
      val paramsJsArray = Json.arr(
        Json.obj("name" → "c", "type" → "boolean"))
      val result = dao.executeRawSql(sqlQuery, paramsJsArray, params)

      result.right.get.result.size mustBe 2
      result.right.get.count mustBe 3

    }

    "only change limit offset to upper-case in original sql during param replacement" in {
      val sqlQuery = "select * from account_types where is_active = {c} or '{c}' = ''"
      val params = Map(
        "c" → "1",
        "Limit" → "2",
        "offset" → "0")
      val expectedSql = "select * from account_types where is_active = {c} or '{c}' = '' LIMIT {limit} OFFSET {offset}"
      val modifiedSql = PrivateMethod[(String, List[JsValue])]('maybeModifiedSql)
      val result = (dao invokePrivate modifiedSql(sqlQuery, params))._1
      expectedSql.contentEquals(result) mustBe true

    }

    "return total 0 if result is emtpy or null" in {
      val sqlQuery = "select * from account_types where 1 = 2"
      val params = Map(
        "b1" → LocalDateTime.now().plusSeconds(10).toString(),
        "p" → "WALLET1",
        "n" → "2",
        "c" → "1")

      val paramsJsArray = Json.arr(
        Json.obj("name" → "b", "type" → "datetime", "default" → "2019-01-01"),
        Json.obj("name" → "b1", "type" → "datetime"),
        Json.obj("name" → "p", "type" → "text"),
        Json.obj("name" → "c", "type" → "boolean"))
      val result = dao.executeRawSql(sqlQuery, paramsJsArray, params)

      result.right.get.count mustBe 0

    }
  }
}
