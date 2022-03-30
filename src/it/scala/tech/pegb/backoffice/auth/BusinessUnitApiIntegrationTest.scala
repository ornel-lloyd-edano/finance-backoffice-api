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

class BusinessUnitApiIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {

  override def cleanupSql =
    s"""
       |DELETE FROM ${BusinessUnitSqlDao.TableName} WHERE id IN ('295e6468-0930-4290-a217-6b9ab204c3ef', '8fe884af-069e-4371-9e2f-678b3e45d2e1', '37d33b2b-7213-4d65-9d36-8532953a89f3');
       |DELETE FROM ${RoleSqlDao.TableName} WHERE id = '7eba8b9b-c43f-4122-976d-c6d76d77890a';
       |DELETE FROM ${BackOfficeUserSqlDao.TableName} WHERE id = '0dc7209a-5588-4d86-affd-1474981e2860';
       |
     """.stripMargin

  override def initSql = //Note: it is good idea to have specific inserts located on the test where it is used
  // rather than have them in init-data.sql because of better readability.
  // Unless inserts provision common data used in many tests
    s"""
       |DELETE FROM ${BusinessUnitSqlDao.TableName} WHERE id IN ('295e6468-0930-4290-a217-6b9ab204c3ef', '8fe884af-069e-4371-9e2f-678b3e45d2e1', '37d33b2b-7213-4d65-9d36-8532953a89f3');
       |DELETE FROM ${RoleSqlDao.TableName} WHERE id = '7eba8b9b-c43f-4122-976d-c6d76d77890a';
       |DELETE FROM ${BackOfficeUserSqlDao.TableName} WHERE id = '0dc7209a-5588-4d86-affd-1474981e2860';
       |
       |INSERT INTO ${BusinessUnitSqlDao.TableName}(id, `name`, is_active, created_by, created_at, updated_by, updated_at)
       |VALUES
       |( '295e6468-0930-4290-a217-6b9ab204c3ef', 'Accounting', '1',  'admin',  '2018-01-01 00:00:00', null, null),
       |( '8fe884af-069e-4371-9e2f-678b3e45d2e1', 'Management', '1',  'admin',  '2018-01-01 00:00:00', null, null),
       |( '37d33b2b-7213-4d65-9d36-8532953a89f3', 'IT',         '1',  'admin',  '2018-01-01 00:00:00', null, null);
       |
       |INSERT INTO ${RoleSqlDao.TableName} (id, `name`, is_active, level, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('7eba8b9b-c43f-4122-976d-c6d76d77890a', 'mock_role', '1',  '1',  'system',  now(), null, null);
       |
       |INSERT INTO ${BackOfficeUserSqlDao.TableName}(id, userName, password, roleId, businessUnitId, email, phoneNumber, firstName, middleName, lastName, description, homePage, activeLanguage, customData, lastLoginTimestamp, is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('0dc7209a-5588-4d86-affd-1474981e2860', 'lloyd', 'password1', '7eba8b9b-c43f-4122-976d-c6d76d77890a',
       |'295e6468-0930-4290-a217-6b9ab204c3ef',
       |'edano@pegb.tech', '0544451678', 'Lloyd', 'Pepito', 'Edano', NULL, NULL, 'Filipino', NULL, NULL, 1, 'pegbuser', 'pegbuser', '2019-10-01 00:00:00', '2019-10-01 00:00:00');
       |
     """.stripMargin

  override val endpoint = s"/business_units"

  "BusinessUnit api" should {
    val db: Database = inject[DBApi].database("backoffice")


    "create business units" in {
      val jsonRequest =
        s"""
           |{"name":"Legal Department"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"internally generated random id will be ignored",
           |"name":"Legal Department",
           |"created_by":"${mockRequestFrom}",
           |"updated_by":null,
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":null
           |}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsJson(resp)
        .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'Legal Department'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isReallyInDB mustBe true
    }

    "fail to create business unit if name is existing" in {
      val jsonRequest =
        s"""
           |{"name":"Legal Department"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CONFLICT

      val isAlreadyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'Legal Department'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isAlreadyInDB mustBe true
    }

    "update the business unit" in {
      val idToUpdate = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'Legal Department'")
        created.executeQuery().as(created.defaultParser.singleOpt).map(r⇒ r[String](BusinessUnitSqlDao.cId))
      }.get

      val jsonRequest =
        s"""
           |{"name":"New Legal Department"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"$idToUpdate",
           |"name":"New Legal Department",
           |"created_by":"${mockRequestFrom}",
           |"updated_by":"${mockRequestFrom}",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson


      val isReallyUpdatedInDB: Boolean = db.withConnection { implicit conn⇒
        val old = SQL(s"SELECT name FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'Legal Department'")
        val isOldFound: Option[Row] = old.executeQuery().as(old.defaultParser.singleOpt)

        val result = SQL(s"SELECT name FROM ${BusinessUnitSqlDao.TableName} " +
          s"WHERE name = 'New Legal Department' " +
          s"AND updated_by = '${mockRequestFrom}' ")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined && isOldFound.isEmpty
      }
      isReallyUpdatedInDB mustBe true
    }

    "fail to update business unit if name is taken" in {
      val (idToUpdate , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'New Legal Department'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](BusinessUnitSqlDao.cId), r[Option[LocalDateTime]](BusinessUnitSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"name":"Accounting",
           |"updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CONFLICT

      val isAlreadyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'Accounting'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isAlreadyInDB mustBe true
    }

    "fail to update business unit if client's version might be stale (through updated_at comparison)" in {
      val (idToUpdate , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'New Legal Department'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](BusinessUnitSqlDao.cId), r[Option[LocalDateTime]](BusinessUnitSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"name":"Accounting",
           |"updated_at":${lastUpdatedAt.get.minusMinutes(1).toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe PRECONDITION_FAILED

      val latestVersionInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT updated_at FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'New Legal Department'")
        result.executeQuery().as(result.defaultParser.singleOpt)
          .flatMap(r⇒ r[Option[LocalDateTime]](BusinessUnitSqlDao.cUpdatedAt))
      }

      (latestVersionInDB.get != lastUpdatedAt.get.minusMinutes(1)) mustBe true
    }

    "get the business unit" in {
      val idToGet = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE " +
          s"name = 'New Legal Department' AND " +
          s"created_by = '$mockRequestFrom' AND " +
          s"updated_by = '$mockRequestFrom' AND " +
          s"created_at = '${mockRequestDate.toLocalDateTimeUTC}' AND " +
          s"updated_at = '${mockRequestDate.toLocalDateTimeUTC}'")
        created.executeQuery().as(created.defaultParser.singleOpt).map(r⇒ r[String](BusinessUnitSqlDao.cId))
      }.get

      val resp = route(app, FakeRequest(GET, s"$endpoint/$idToGet")).get
      val expectedJson =
        s"""
           |{"id":"${idToGet}",
           |"name":"New Legal Department",
           |"created_by":"$mockRequestFrom",
           |"updated_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }

    "get all business units" in {
      val idOfUpdated = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'New Legal Department'")
        result.executeQuery().as(result.defaultParser.singleOpt).map(r⇒ r[String](BusinessUnitSqlDao.cId))
      }.get
      val resp = route(app, FakeRequest(GET, s"$endpoint?order_by=name&limit=4&offset=0")).get
      val expectedJson =
        s"""
           |{
           |"total":4,
           |"results":[
           |{"id":"295e6468-0930-4290-a217-6b9ab204c3ef",
           |"name":"Accounting",
           |"created_by":"admin",
           |"updated_by":null,
           |"created_at":"2018-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2018-01-01T00:00:00Z",
           |"updated_time":null
           |},
           |{"id":"37d33b2b-7213-4d65-9d36-8532953a89f3",
           |"name":"IT",
           |"created_by":"admin",
           |"updated_by":null,
           |"created_at":"2018-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2018-01-01T00:00:00Z",
           |"updated_time":null},
           |{"id":"8fe884af-069e-4371-9e2f-678b3e45d2e1",
           |"name":"Management",
           |"created_by":"admin",
           |"updated_by":null,
           |"created_at":"2018-01-01T00:00:00Z",
           |"updated_at":null,
           |"created_time":"2018-01-01T00:00:00Z",
           |"updated_time":null
           |},
           |{"id":"$idOfUpdated",
           |"name":"New Legal Department",
           |"created_by":"$mockRequestFrom",
           |"updated_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
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
        val updated = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'New Legal Department'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](BusinessUnitSqlDao.cId), r[Option[LocalDateTime]](BusinessUnitSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}}
         """.stripMargin

      val resp = route(app, FakeRequest(DELETE, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(resp) mustBe OK //NO_CONTENT

      val isNotFound = route(app, FakeRequest(GET, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(isNotFound) mustBe NOT_FOUND
    }

    "fail to remove the business unit if it is still being used by a back_office_user" in {
      val (idToBeDeleted , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${BusinessUnitSqlDao.TableName} WHERE name = 'Accounting'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](BusinessUnitSqlDao.cId), r[Option[LocalDateTime]](BusinessUnitSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"updated_at":null
         """.stripMargin

      val resp = route(app, FakeRequest(DELETE, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(resp) mustBe BAD_REQUEST

      val backOfficeUserUnderBusinessUnitExists = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cBuId} = '$idToBeDeleted'")
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
