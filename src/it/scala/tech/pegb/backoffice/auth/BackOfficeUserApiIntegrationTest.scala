package tech.pegb.backoffice.auth

import java.time.LocalDateTime
import java.util.UUID

import anorm._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.{DBApi, Database}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.PlayIntegrationTest
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.dao.auth.sql.{BackOfficeUserSqlDao, BusinessUnitSqlDao, RoleSqlDao}
import tech.pegb.backoffice.util.Implicits._

class BackOfficeUserApiIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  override def cleanupSql: String =
    s"""
       |DELETE FROM ${BusinessUnitSqlDao.TableName} WHERE id IN ('295e6468-0930-4290-a217-6b9ab204c3ef', '8fe884af-069e-4371-9e2f-678b3e45d2e1', '37d33b2b-7213-4d65-9d36-8532953a89f3');
       |DELETE FROM ${RoleSqlDao.TableName} WHERE id = '7eba8b9b-c43f-4122-976d-c6d76d77890a';
       |DELETE FROM ${BackOfficeUserSqlDao.TableName};
       |
     """.stripMargin

  override def initSql: String = //Note: it is good idea to have specific inserts located on the test where it is used
  // rather than have them in init-data.sql because of better readability.
  // Unless inserts provision common data used in many tests
    s"""
       |INSERT INTO ${BusinessUnitSqlDao.TableName}(id, `name`, is_active, created_by, created_at, updated_by, updated_at)
       |VALUES
       |( '295e6468-0930-4290-a217-6b9ab204c3ef', 'Accounting', '1',  'admin',  '2018-01-01 00:00:00', null, null),
       |( '8fe884af-069e-4371-9e2f-678b3e45d2e1', 'Management', '1',  'admin',  '2018-01-01 00:00:00', null, null),
       |( '37d33b2b-7213-4d65-9d36-8532953a89f3', 'IT',         '1',  'admin',  '2018-01-01 00:00:00', null, null);
       |
       |INSERT INTO ${RoleSqlDao.TableName} (id, `name`, is_active, level, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('7eba8b9b-c43f-4122-976d-c6d76d77890a', 'mock_role', '1',  '1',  'system',  '2018-01-01 00:00:00', null, null);
       |
       |INSERT INTO ${BackOfficeUserSqlDao.TableName}(id, userName, password, roleId, businessUnitId, email, phoneNumber, firstName, middleName, lastName, description, homePage, activeLanguage, customData, lastLoginTimestamp, is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('0dc7209a-5588-4d86-affd-1474981e2860', 'lloyd', 'password1', '7eba8b9b-c43f-4122-976d-c6d76d77890a',
       |'295e6468-0930-4290-a217-6b9ab204c3ef',
       |'edano@pegb.tech', '0544451678', 'Lloyd', 'Pepito', 'Edano', NULL, NULL, 'Filipino', NULL, NULL, 1, 'pegbuser', 'pegbuser', '2019-10-01 00:00:00', '2019-10-01 00:00:00');
       |
     """.stripMargin

  override val endpoint = s"/back_office_users"

  "BackofficeUsers api" should {
    val db: Database = inject[DBApi].database("backoffice")

    "create backoffice users" in {

      val jsonRequest =
        s"""
           |{
           |  "user_name": "ujalityagi",
           |  "email": "u.tyagi@pegb.tech",
           |  "phone_number": "0582181475",
           |  "first_name": "ujali",
           |  "middle_name": "",
           |  "last_name": "tyagi",
           |  "description": "create back office user it test",
           |  "home_page": "",
           |  "active_language": "Hindi",
           |  "custom_data": null,
           |  "role_id": "7eba8b9b-c43f-4122-976d-c6d76d77890a",
           |  "business_unit_id": "295e6468-0930-4290-a217-6b9ab204c3ef"
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val respF = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{
           |	"last_login_timestamp": null,
           |	"created_time": ${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |	"updated_time": ${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |	"role": {
           |		"id": "7eba8b9b-c43f-4122-976d-c6d76d77890a",
           |		"name": "mock_role",
           |		"level": 1,
           |		"created_by": "system",
           |		"updated_by": null,
           |		"created_at": "2018-01-01T00:00:00Z",
           |		"updated_at": null,
           |		"created_time": "2018-01-01T00:00:00Z",
           |		"updated_time": null
           |	},
           |	"user_name": "ujalityagi",
           |	"last_name": "tyagi",
           |	"description": "create back office user it test",
           |	"created_at": ${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |	"middle_name": "",
           |	"business_unit": {
           |		"id": "295e6468-0930-4290-a217-6b9ab204c3ef",
           |		"name": "Accounting",
           |		"created_by": "admin",
           |		"updated_by": null,
           |		"created_at": "2018-01-01T00:00:00Z",
           |		"updated_at": null,
           |		"created_time": "2018-01-01T00:00:00Z",
           |		"updated_time": null
           |	},
           |	"created_by": "pegbuser",
           |	"active_language": "Hindi",
           |	"home_page": "",
           |	"updated_at": ${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |	"permissions": [],
           |	"updated_by": "pegbuser",
           |	"phone_number": "0582181475",
           |	"id": "internally generated random id will be ignored",
           |	"first_name": "ujali",
           |	"custom_data": null,
           |	"email": "u.tyagi@pegb.tech"
           |}
         """.stripMargin

      whenReady(respF) { _ ⇒
        status(respF) mustBe CREATED
        contentAsJson(respF)
          .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


        val isReallyInDB: Boolean = db.withConnection { implicit conn ⇒
          val result = SQL(s"SELECT ${BackOfficeUserSqlDao.cUsername} FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cUsername} = 'ujalityagi'")
          val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
          found.isDefined
        }
        isReallyInDB mustBe true
      }
    }

    "fail to create backoffice users" in {

      val jsonRequest =
        s"""
           |{
           |  "user_name": "ujalityagi",
           |  "business_unit_id": "295e6468-0930-4290-a217-6b9ab204c3ef"
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val respF = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get

      val expectedJson =
        s"""
           |{
           |	"id": "internally generated random id will be ignored",
           |	"code": "MalformedRequest",
           |	"msg": "Malformed request to create back_office_user. A mandatory field might be missing or its value is of wrong type."
           |}
         """.stripMargin

      whenReady(respF) { _ ⇒
        status(respF) mustBe BAD_REQUEST
        contentAsJson(respF)
          .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]
      }

    }

    "get backoffice user by id" in {

      val respF = route(app, FakeRequest(GET, s"$endpoint/0dc7209a-5588-4d86-affd-1474981e2860")).get

      val expectedJson =
        s"""
           |{
           |	"last_login_timestamp": null,
           |	"created_time": "2019-10-01T00:00:00Z",
           |	"updated_time": "2019-10-01T00:00:00Z",
           |	"role": {
           |		"id": "7eba8b9b-c43f-4122-976d-c6d76d77890a",
           |		"name": "mock_role",
           |		"level": 1,
           |		"created_by": "system",
           |		"updated_by": null,
           |		"created_at": "2018-01-01T00:00:00Z",
           |		"updated_at": null,
           |		"created_time": "2018-01-01T00:00:00Z",
           |		"updated_time": null
           |	},
           |	"user_name": "lloyd",
           |	"last_name": "Edano",
           |	"description": null,
           |	"created_at": "2019-10-01T00:00:00Z",
           |	"middle_name": "Pepito",
           |	"business_unit": {
           |		"id": "295e6468-0930-4290-a217-6b9ab204c3ef",
           |		"name": "Accounting",
           |		"created_by": "admin",
           |		"updated_by": null,
           |		"created_at": "2018-01-01T00:00:00Z",
           |		"updated_at": null,
           |		"created_time": "2018-01-01T00:00:00Z",
           |		"updated_time": null
           |	},
           |	"created_by": "pegbuser",
           |	"active_language": "Filipino",
           |	"home_page": null,
           |	"updated_at": "2019-10-01T00:00:00Z",
           |	"permissions": [],
           |	"updated_by": "pegbuser",
           |	"phone_number": "0544451678",
           |	"id": "internally generated random id will be ignored",
           |	"first_name": "Lloyd",
           |	"custom_data": null,
           |	"email": "edano@pegb.tech"
           |}
         """.stripMargin

      whenReady(respF) { response ⇒

        status(respF) mustBe OK
        contentAsJson(respF)
          .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


        val isReallyInDB: Boolean = db.withConnection { implicit conn ⇒
          val result = SQL(s"SELECT ${BackOfficeUserSqlDao.cId} FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cId} = '0dc7209a-5588-4d86-affd-1474981e2860'")
          val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
          found.isDefined
        }
        isReallyInDB mustBe true
      }


    }

    "get backoffice users by criteria" in {

      val respF = route(app, FakeRequest(GET, s"$endpoint?first_name=Lloyd")).get

      val expectedJson =
        s"""
           |{
           |	"total": 1,
           |	"offset": null,
           |	"limit": null,
           |	"id": "internally generated random id will be ignored",
           |	"results": [{
           |		"id": "0dc7209a-5588-4d86-affd-1474981e2860",
           |		"user_name": "lloyd",
           |		"email": "edano@pegb.tech",
           |		"phone_number": "0544451678",
           |		"first_name": "Lloyd",
           |		"middle_name": "Pepito",
           |		"last_name": "Edano",
           |		"description": null,
           |		"home_page": null,
           |		"active_language": "Filipino",
           |		"last_login_timestamp": null,
           |		"custom_data": null,
           |		"role": {
           |			"id": "7eba8b9b-c43f-4122-976d-c6d76d77890a",
           |			"name": "mock_role",
           |			"level": 1,
           |			"created_by": "system",
           |			"updated_by": null,
           |			"created_at": "2018-01-01T00:00:00Z",
           |			"updated_at": null,
           |			"created_time": "2018-01-01T00:00:00Z",
           |			"updated_time": null
           |		},
           |		"business_unit": {
           |			"id": "295e6468-0930-4290-a217-6b9ab204c3ef",
           |			"name": "Accounting",
           |			"created_by": "admin",
           |			"updated_by": null,
           |			"created_at": "2018-01-01T00:00:00Z",
           |			"updated_at": null,
           |			"created_time": "2018-01-01T00:00:00Z",
           |			"updated_time": null
           |		},
           |		"permissions": [],
           |		"created_by": "pegbuser",
           |		"updated_by": "pegbuser",
           |		"created_at": "2019-10-01T00:00:00Z",
           |		"updated_at": "2019-10-01T00:00:00Z",
           |		"created_time": "2019-10-01T00:00:00Z",
           |		"updated_time": "2019-10-01T00:00:00Z"
           |	}]
           |}
         """.stripMargin

      whenReady(respF) { _ ⇒

        status(respF) mustBe OK
        contentAsJson(respF)
          .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


        val isReallyInDB: Boolean = db.withConnection { implicit conn ⇒
          val result = SQL(s"SELECT ${BackOfficeUserSqlDao.cId} FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cId} = '0dc7209a-5588-4d86-affd-1474981e2860'")
          val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
          found.isDefined
        }
        isReallyInDB mustBe true
      }
    }


    "update backoffice user" in {

      val (idToUpdate, lastUpdatedAt) = db.withConnection { implicit conn ⇒
        val created = SQL(s"SELECT * FROM ${BackOfficeUserSqlDao.TableName} WHERE  ${BackOfficeUserSqlDao.cUsername} = 'lloyd'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r ⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val json =
        s"""
           |{
           |  "email": "lloyd_edano@pegb.tech",
           | "updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin

      val respF = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, json)).get
      val expectedJson =
        s"""
           |{
           |	"last_login_timestamp": null,
           |	"created_time": "2019-10-01T00:00:00Z",
           |	"updated_time": ${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |	"role": {
           |		"id": "7eba8b9b-c43f-4122-976d-c6d76d77890a",
           |		"name": "mock_role",
           |		"level": 1,
           |		"created_by": "system",
           |		"updated_by": null,
           |		"created_at": "2018-01-01T00:00:00Z",
           |		"updated_at": null,
           |		"created_time": "2018-01-01T00:00:00Z",
           |		"updated_time": null
           |	},
           |	"user_name": "lloyd",
           |	"last_name": "Edano",
           |	"description": null,
           |	"created_at": "2019-10-01T00:00:00Z",
           |	"middle_name": "Pepito",
           |	"business_unit": {
           |		"id": "295e6468-0930-4290-a217-6b9ab204c3ef",
           |		"name": "Accounting",
           |		"created_by": "admin",
           |		"updated_by": null,
           |		"created_at": "2018-01-01T00:00:00Z",
           |		"updated_at": null,
           |		"created_time": "2018-01-01T00:00:00Z",
           |		"updated_time": null
           |	},
           |	"created_by": "pegbuser",
           |	"active_language": "Filipino",
           |	"home_page": null,
           |	"updated_at": ${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |	"permissions": [],
           |	"updated_by": "pegbuser",
           |	"phone_number": "0544451678",
           |	"id": "internally generated random id will be ignored",
           |	"first_name": "Lloyd",
           |	"custom_data": null,
           |	"email": "lloyd_edano@pegb.tech"
           |}
         """.stripMargin

      whenReady(respF) { _ ⇒

        status(respF) mustBe OK
        contentAsJson(respF)
          .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


        val isReallyInDB: Boolean = db.withConnection { implicit conn ⇒
          val result = SQL(s"SELECT ${BackOfficeUserSqlDao.cId} FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cId} = '0dc7209a-5588-4d86-affd-1474981e2860'")
          val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
          found.isDefined
        }
        isReallyInDB mustBe true
      }
    }


    "fail to update backoffice users" in {

      val (idToUpdate, lastUpdatedAt) = db.withConnection { implicit conn ⇒
        val created = SQL(s"SELECT * FROM ${BackOfficeUserSqlDao.TableName} WHERE  ${BackOfficeUserSqlDao.cUsername} = 'lloyd'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r ⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val json =
        s"""
           |{
           |  "email": "u.tyagi@pegb.tech@pegb.tech",
           | "updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin

      val respF = route(app, FakeRequest(POST, s"$endpoint/$idToUpdate", jsonHeaders, json)).get

      val expectedJson =
        s"""
           |{
           |	"msg": "",
           |	"code": "Unknown",
           |	"id": "internally generated random id will be ignored"
           |}
         """.stripMargin

      whenReady(respF) { _ ⇒
        status(respF) mustBe NOT_FOUND
        contentAsJson(respF)
          .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]
      }

    }

    "delete backoffice user by id" in {

      val (idToUpdate, lastUpdatedAt) = db.withConnection { implicit conn ⇒
        val created = SQL(s"SELECT * FROM ${BackOfficeUserSqlDao.TableName} WHERE  ${BackOfficeUserSqlDao.cUsername} = 'lloyd'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r ⇒ (r[String](RoleSqlDao.cId), r[Option[LocalDateTime]](RoleSqlDao.cUpdatedAt)))
      }.get

      val json =
        s"""
           |{
           | "updated_at":${lastUpdatedAt.get.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin

      val respF = route(app, FakeRequest(DELETE, s"$endpoint/$idToUpdate", jsonHeaders, json)).get

      whenReady(respF) { _ ⇒
        status(respF) mustBe OK
        contentAsString(respF) contains  "0dc7209a-5588-4d86-affd-1474981e2860"


        val isDeleted: Boolean = db.withConnection { implicit conn ⇒
          val result = SQL(s"SELECT ${BackOfficeUserSqlDao.cId} FROM ${BackOfficeUserSqlDao.TableName} WHERE ${BackOfficeUserSqlDao.cId} = '0dc7209a-5588-4d86-affd-1474981e2860' AND ${BackOfficeUserSqlDao.cIsActive}=1")
          val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)

          found.isEmpty
        }
        isDeleted mustBe true
      }
    }


  }
}
