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
import tech.pegb.backoffice.dao.auth.sql._
import tech.pegb.backoffice.util.Implicits._

class ScopesApiIntegrationTest extends PlayIntegrationTest with MockFactory with ScalaFutures {

  override def cleanupSql =
    s"""
       |DELETE FROM ${PermissionSqlDao.TableName} WHERE id = 'b791a225-60bb-4828-9801-7f6620466b5e';
       |
       |DELETE FROM ${ScopeSqlDao.TableName} WHERE name IN
       |('customers',
       |'customers_detail',
       |'customers_edit',
       |'customers_delete',
       |'customers_create');
       |
     """.stripMargin

  override def initSql = //Note: it is good idea to have specific inserts located on the test where it is used
  // rather than have them in init-data.sql because of better readability.
  // Unless inserts provision common data used in many tests

  //TODO remove status, cBy, uBy, cDate, uDate,  later when standalone backoffice auth is taken down
    s"""
       |DELETE FROM ${ScopeSqlDao.TableName} WHERE id IN
       |('48238e01-5694-4773-b490-5e5e069e2d98', '295e6468-0930-4290-a217-6b9ab204c3ef', '8fe884af-069e-4371-9e2f-678b3e45d2e1', '66a352c3-7d21-429c-af71-cf7bf7aacddc');
       |
       |INSERT INTO ${ScopeSqlDao.TableName}(id, parentId,                                `name`,             description, status, cBy,     uBy,     cDate,                 uDate,                   is_active,  created_by,  updated_by,  created_at,  updated_at)
       |VALUES
       |( '48238e01-5694-4773-b490-5e5e069e2d98', null,                                   'customers',        null,        b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00',  b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00'),
       |( '295e6468-0930-4290-a217-6b9ab204c3ef', '48238e01-5694-4773-b490-5e5e069e2d98', 'customers_detail', null,        b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00',  b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00'),
       |( '8fe884af-069e-4371-9e2f-678b3e45d2e1', '48238e01-5694-4773-b490-5e5e069e2d98', 'customers_edit',   null,        b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00',  b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00'),
       |( '66a352c3-7d21-429c-af71-cf7bf7aacddc', '48238e01-5694-4773-b490-5e5e069e2d98', 'customers_delete', null,        b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00',  b'1',   'admin',  'admin', '2018-01-01 00:00:00', '2018-01-01 00:00:00');
       |
       |INSERT INTO ${PermissionSqlDao.TableName} (id,buId,userId,roleId,scopeId,canWrite,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('b791a225-60bb-4828-9801-7f6620466b5e',NULL,NULL,NULL,
       |'295e6468-0930-4290-a217-6b9ab204c3ef',
       |1,1,'system','system','2018-01-01 00:00:00','2018-01-01 00:00:00');
     """.stripMargin

  override val endpoint = s"/scopes"

  "Scopes api" should {
    val db: Database = inject[DBApi].database("backoffice") //TODO maybe promote to PlayIntegrationTest

    "create scopes" in {
      val jsonRequest =
        s"""
           |{"name":"customers_create",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":"scope for POST /api/customers"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"internally generated random id will be ignored",
           |"name":"customers_create",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":"scope for POST /api/customers",
           |"created_by":"${mockRequestFrom}",
           |"updated_by":"${mockRequestFrom}",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsJson(resp)
        .as[JsObject].+("id" → JsString("internally generated random id will be ignored")) mustBe Json.parse(expectedJson).as[JsObject]


      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isReallyInDB mustBe true
    }

    "fail to create scopes if name is existing" in {
      val jsonRequest =
        s"""
           |{"name":"customers_create"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$endpoint", jsonHeaders, jsonRequest)).get

      status(resp) mustBe CONFLICT

      val isAlreadyInDB: Boolean = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT name FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined
      }
      isAlreadyInDB mustBe true
    }

    "update the scopes" in {
      val (idToUpdate, createdBy, createdAt, updatedAt) = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r⇒ (r[String](ScopeSqlDao.cId), r[Option[String]](ScopeSqlDao.cCreatedBy), r[Option[LocalDateTime]](ScopeSqlDao.cCreatedAt), r[Option[LocalDateTime]](ScopeSqlDao.cUpdatedAt)) )
      }.get

      val jsonRequest =
        s"""
           |{"description":"can be used on all endpoints that create customers",
           |"updated_at":${updatedAt.get.toZonedDateTimeUTC.toJsonStr}}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get
      val expectedJson =
        s"""
           |{"id":"$idToUpdate",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":"can be used on all endpoints that create customers",
           |"name":"customers_create",
           |"created_by":"${createdBy.get}",
           |"created_at":${createdAt.get.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"${mockRequestFrom}",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${createdAt.get.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsJson(resp).as[JsObject] mustBe Json.parse(expectedJson).as[JsObject] //I dont want to care about the order of fields if comparing as strings

      val isReallyUpdatedInDB: Boolean = db.withConnection { implicit conn⇒
        val old = SQL(s"SELECT name FROM ${ScopeSqlDao.TableName} " +
          s"WHERE name = 'customers_create' AND description = 'scope for POST /api/customers'")
        val isOldFound: Option[Row] = old.executeQuery().as(old.defaultParser.singleOpt)

        val result = SQL(s"SELECT name FROM ${ScopeSqlDao.TableName} " +
          s"WHERE name = 'customers_create' " +
          s"AND updated_by = '${mockRequestFrom}' ")
        val found: Option[Row] = result.executeQuery().as(result.defaultParser.singleOpt)
        found.isDefined && isOldFound.isEmpty
      }
      isReallyUpdatedInDB mustBe true
    }

    "fail to update scope if client's version might be stale (through updated_at comparison)" in {
      val (idToUpdate , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](ScopeSqlDao.cId), r[Option[LocalDateTime]](ScopeSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"name":"customers_save",
           |"updated_at":${lastUpdatedAt.get.minusMinutes(1).toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$endpoint/$idToUpdate", jsonHeaders, jsonRequest)).get

      status(resp) mustBe PRECONDITION_FAILED

      val latestVersionInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT updated_at FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        result.executeQuery().as(result.defaultParser.singleOpt)
          .flatMap(r⇒ r[Option[LocalDateTime]](ScopeSqlDao.cUpdatedAt))
      }

      (latestVersionInDB.get != lastUpdatedAt.get.minusMinutes(1)) mustBe true
    }

    "get the scope" in {
      val (idToGet, createdAt, updatedAt) = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r⇒ (r[String](ScopeSqlDao.cId), r[Option[LocalDateTime]](ScopeSqlDao.cCreatedAt), r[Option[LocalDateTime]](ScopeSqlDao.cUpdatedAt)) )
      }.get

      val resp = route(app, FakeRequest(GET, s"$endpoint/$idToGet")).get
      val expectedJson =
        s"""
           |{"id":"$idToGet",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":"can be used on all endpoints that create customers",
           |"name":"customers_create",
           |"created_by":"pegbuser",
           |"created_at":${createdAt.get.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${updatedAt.get.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${createdAt.get.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${updatedAt.get.toZonedDateTimeUTC.toJsonStr}}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsJson(resp).as[JsObject] mustBe Json.parse(expectedJson).as[JsObject]
    }

    "get all scopes" in {
      val (idToOfInserted, createdAtOfInserted, updatedAtOfUpdated) = db.withConnection { implicit conn⇒
        val created = SQL(s"SELECT * FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        created.executeQuery().as(created.defaultParser.singleOpt)
          .map(r⇒ (r[String](ScopeSqlDao.cId), r[Option[LocalDateTime]](ScopeSqlDao.cCreatedAt), r[Option[LocalDateTime]](ScopeSqlDao.cUpdatedAt)) )
      }.get
      val resp = route(app, FakeRequest(GET, s"$endpoint?order_by=name&limit=4&offset=0")).get
      val expectedJson =
        s"""
           |{"total":5,
           |"results":[
           |{"id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"name":"customers",
           |"parent_id":null,
           |"description":null,
           |"created_by":"admin",
           |"created_at":"2018-01-01T00:00:00Z",
           |"updated_by":"admin",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"created_time":"2018-01-01T00:00:00Z",
           |"updated_time":"2018-01-01T00:00:00Z"
           |},
           |{"id":"$idToOfInserted",
           |"name":"customers_create",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":"can be used on all endpoints that create customers",
           |"created_by":"pegbuser",
           |"created_at":${createdAtOfInserted.get.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${updatedAtOfUpdated.get.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${createdAtOfInserted.get.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${updatedAtOfUpdated.get.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"66a352c3-7d21-429c-af71-cf7bf7aacddc",
           |"name":"customers_delete",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":null,
           |"created_by":"admin",
           |"created_at":"2018-01-01T00:00:00Z",
           |"updated_by":"admin",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"created_time":"2018-01-01T00:00:00Z",
           |"updated_time":"2018-01-01T00:00:00Z"
           |},
           |{"id":"295e6468-0930-4290-a217-6b9ab204c3ef",
           |"name":"customers_detail",
           |"parent_id":"48238e01-5694-4773-b490-5e5e069e2d98",
           |"description":null,
           |"created_by":"admin",
           |"created_at":"2018-01-01T00:00:00Z",
           |"updated_by":"admin",
           |"updated_at":"2018-01-01T00:00:00Z",
           |"created_time":"2018-01-01T00:00:00Z",
           |"updated_time":"2018-01-01T00:00:00Z"}],
           |"limit":4,"offset":0}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "remove the scope" in {
      val (idToBeDeleted , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_create'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](ScopeSqlDao.cId), r[Option[LocalDateTime]](ScopeSqlDao.cUpdatedAt)))
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

    "fail to remove the scope if it is still being used in permissions" in {
      val (idToBeDeleted , lastUpdatedAt) = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${ScopeSqlDao.TableName} WHERE name = 'customers_detail'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ (r[String](BusinessUnitSqlDao.cId), r[Option[LocalDateTime]](BusinessUnitSqlDao.cUpdatedAt)))
      }.get

      val jsonRequest =
        s"""
           |{"updated_at":${lastUpdatedAt.map(_.toZonedDateTimeUTC.toJsonStr).getOrElse("null")}
         """.stripMargin

      val resp = route(app, FakeRequest(DELETE, s"$endpoint/$idToBeDeleted", jsonHeaders, jsonRequest)).get
      status(resp) mustBe BAD_REQUEST

      val permissionWithThisRoleExists = db.withConnection { implicit conn⇒
        val updated = SQL(s"SELECT * FROM ${PermissionSqlDao.TableName} WHERE ${PermissionSqlDao.cScopeId} = '$idToBeDeleted'")
        updated.executeQuery().as(updated.defaultParser.singleOpt)
          .map(r⇒ r[String](PermissionSqlDao.cId)).isDefined
      }

      permissionWithThisRoleExists mustBe true
    }
    "clean up" in { //because afterAll does not work (crashes the test for some reason)
      executeCleanUp(db)
    }
  }

}
