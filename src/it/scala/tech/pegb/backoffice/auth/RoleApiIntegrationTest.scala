package tech.pegb.backoffice.auth

import java.time.LocalDateTime

import anorm._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.db.{DBApi, Database}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.PlayIntegrationTest
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.dao.auth.sql.{BackOfficeUserSqlDao, BusinessUnitSqlDao, RoleSqlDao}
import tech.pegb.backoffice.util.Implicits._

class RoleApiIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {

  override def cleanupSql =
    s"""
       |DELETE FROM ${RoleSqlDao.TableName} WHERE id IN ('cdf939d7-dd53-4638-9623-7fab6ecb8ab0', '083ac3a1-48f2-40eb-aad8-0fbdbfb5651c', 'babf5f9a-6f77-439c-a147-0e702e5e968f');
       |DELETE FROM ${BusinessUnitSqlDao.TableName} WHERE id = '85500b8f-eb1d-43ab-a1de-7b70fdccf73c';
       |DELETE FROM ${BackOfficeUserSqlDao.TableName} WHERE id = '0dc7209a-5588-4d86-affd-1474981e2860';
     """.stripMargin

  override def initSql = //Note: it is good idea to have specific inserts located on the test where it is used
  // rather than have them in init-data.sql because of better readability.
  // Unless inserts provision common data used in many tests
    s"""
       |DELETE FROM ${RoleSqlDao.TableName} WHERE id IN ('cdf939d7-dd53-4638-9623-7fab6ecb8ab0', '083ac3a1-48f2-40eb-aad8-0fbdbfb5651c', 'babf5f9a-6f77-439c-a147-0e702e5e968f');
       |DELETE FROM ${BusinessUnitSqlDao.TableName} WHERE id = '85500b8f-eb1d-43ab-a1de-7b70fdccf73c';
       |DELETE FROM ${BackOfficeUserSqlDao.TableName} WHERE id = '0dc7209a-5588-4d86-affd-1474981e2860';
       |
       |INSERT INTO ${BusinessUnitSqlDao.TableName}(id, `name`, is_active, created_by, created_at, updated_by, updated_at)
       |VALUES
       |( '85500b8f-eb1d-43ab-a1de-7b70fdccf73c', 'Accounting', '1',  'admin',  '2018-01-01 00:00:00', null, null);
       |
       |INSERT INTO ${RoleSqlDao.TableName} (id, `name`, is_active, level, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('cdf939d7-dd53-4638-9623-7fab6ecb8ab0', 'Manager',    '1',  '1',  'system',  '2019-01-01 00:00:00', null, null),
       |('083ac3a1-48f2-40eb-aad8-0fbdbfb5651c', 'Supervisor', '1',  '2',  'system',  '2019-01-01 00:00:00', null, null),
       |('babf5f9a-6f77-439c-a147-0e702e5e968f', 'Assistant',  '1',  '3',  'system',  '2019-01-01 00:00:00', null, null);
       |
       |INSERT INTO ${BackOfficeUserSqlDao.TableName}(id, userName, password, roleId, businessUnitId, email, phoneNumber, firstName, middleName, lastName, description, homePage, activeLanguage, customData, lastLoginTimestamp, is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('0dc7209a-5588-4d86-affd-1474981e2860', 'lloyd', 'password1',
       |'cdf939d7-dd53-4638-9623-7fab6ecb8ab0',
       |'85500b8f-eb1d-43ab-a1de-7b70fdccf73c', 'edano@pegb.tech', '0544451678', 'Lloyd', 'Pepito', 'Edano', NULL, NULL, 'Filipino', NULL, NULL, 1, 'pegbuser', 'pegbuser', '2019-10-01 00:00:00', '2019-10-01 00:00:00');
       |
     """.stripMargin

  override val endpoint = "/roles"

  "Role api" should {
    val db: Database = inject[DBApi].database("backoffice")

    "create roles" in {
      val jsonRequest =
        s"""
           |{"name":"Normal Employee",
           |"level":3}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"internally generated random id will be ignored",
           |"name":"Normal Employee",
           |"level":3,
           |"created_by":"$mockRequestFrom",
           |"updated_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsJson(resp)
        .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${RoleSqlDao.TableName} WHERE name = 'Normal Employee'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isReallyInDB mustBe true
    }

    "fail to create role if name is existing" in {
      val jsonRequest =
        s"""
           |{"name":"Normal Employee"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CONFLICT

      val isAlreadyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${RoleSqlDao.TableName} WHERE name = 'Normal Employee'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isAlreadyInDB mustBe true
    }

    "update the role" in {
      val (idToUpdate, lastUpdatedAt) = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Normal Employee'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"name":"Super Employee",
           |"level":1,
           |"updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"internally generated random id will be ignored",
           |"name":"Super Employee",
           |"level":1,
           |"created_by":"${mockRequestFrom}",
           |"updated_by":"${mockRequestFrom}",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsJson(resp)
        .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


      val isReallyUpdatedInDB: Boolean = db.withConnection { implicit conn⇒
        val old = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Normal Employee'")
        val isOldFound: Option[Row] = old.executeQuery().as(old.defaultParser.singleOpt)

        val result = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} " +
          s"WHERE name = 'Super Employee' " +
          s"AND updated_by = '${mockRequestFrom}' ")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined && isOldFound.isEmpty
      }
      isReallyUpdatedInDB mustBe true
    }

    "fail to update role if name is taken" in {  //TODO fix needed from Ujali
      val (idToUpdate , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Super Employee'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"name":"Manager",
           |"updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CONFLICT

      val isAlreadyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${RoleSqlDao.TableName} WHERE name = 'Manager'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isAlreadyInDB mustBe true
    }

    "fail to update role if client's version might be stale (through updated_at comparison)" in {  //TODO fix needed from Ujali
      val (idToUpdate , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Super Employee'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"name":"Mega Employee",
           |"updated_at":${lastUpdatedAt.get.minusMinutes(1).toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe PRECONDITION_FAILED

      val latestVersionInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT updated_at FROM ${RoleSqlDao.TableName} WHERE name = 'Super Employee'")
        result.executeQuery().as(result.defaultParser.singleOpt)
          .flatMap(r⇒ r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt))
      }

      (latestVersionInDB.get != lastUpdatedAt.get.minusMinutes(1)) mustBe true
    }

    "get the role" in {
      val idToGet = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Assistant'")
        created.executeQuery().as(created.defaultParser.singleOpt).map(r⇒ r[String](RoleSqlDao.cId))
      }.get

      val resp = route(app, FakeRequest(GET, s"$endpoint/$idToGet")).get
      val expectedJson =
        s"""
           |{"id":"${idToGet}",
           |"name":"Assistant",
           |"level":3,
           |"created_by":"system",
           |"updated_by":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2019-01-01T00:00:00Z",
           |"updated_time":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "get all roles" in { //TODO update Super Employee to Mega Employee once the tests above succeed
      val (idOfUpdated, lastUpdatedAt) = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Super Employee'")
        result.executeQuery().as(result.defaultParser.singleOpt)
          .map(r⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)) )
      }.get
      val resp = route(app, FakeRequest(GET, s"$endpoint?order_by=name&limit=4&offset=0")).get

      val expectedJson =
        s"""
           |{
           |"total":4,
           |"results":[
           |{"id":"babf5f9a-6f77-439c-a147-0e702e5e968f",
           |"name":"Assistant",
           |"level":3,
           |"created_by":"system",
           |"updated_by":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2019-01-01T00:00:00Z",
           |"updated_time":null
           |},
           |{"id":"cdf939d7-dd53-4638-9623-7fab6ecb8ab0",
           |"name":"Manager",
           |"level":1,
           |"created_by":"system",
           |"updated_by":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2019-01-01T00:00:00Z",
           |"updated_time":null
           |},
           |{"id":"$idOfUpdated",
           |"name":"Super Employee",
           |"level":1,
           |"created_by":"$mockRequestFrom",
           |"updated_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"083ac3a1-48f2-40eb-aad8-0fbdbfb5651c",
           |"name":"Supervisor",
           |"level":2,
           |"created_by":"system",
           |"updated_by":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2019-01-01T00:00:00Z",
           |"updated_time":null
           |}
           |],
           |"limit":4,
           |"offset":0
           |}
           |""".stripMargin.replace(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "remove the business unit" in {
      val (idToBeDeleted , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Assistant'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"updated_at":${lastUpdatedAt.map(_.toZonedDateTimeUTC.toJsonStr).getOrElse("null")}}
         """.stripMargin

      val resp = route(app, FakeRequest(DELETE, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(resp) mustBe OK //NO_CONTENT

      val isNotFound = route(app, FakeRequest(GET, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(isNotFound) mustBe NOT_FOUND
    }

    "fail to remove the business unit if it is still being used by a back_office_user" in {
      val (idToBeDeleted , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${RoleSqlDao.TableName} WHERE name = 'Manager'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"updated_at":null
         """.stripMargin

      val resp = route(app, FakeRequest(DELETE, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(resp) mustBe BAD_REQUEST

      val backOfficeUserUnderBusinessUnitExists = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cRoleId} = '$idToBeDeleted'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ r[String](BackOfficeUserSqlDao.cId)).isDefined
      }

      backOfficeUserUnderBusinessUnitExists mustBe true
    }
    "clean up" in { //because afterAll does not work (crashes the test for some reason)
      executeCleanUp(db)
    }
  }

}
