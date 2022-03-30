package tech.pegb.backoffice

import java.time._
import java.util.UUID

import anorm.{SQL, _}
import play.api.db.DBApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.application.dto.WalletApplicationDetail
import tech.pegb.backoffice.api.json.Implicits._

import scala.concurrent.{ExecutionContext}

class WalletApplicationIntegrationTest extends PlayIntegrationTest {
  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  private val basePath = "/api/wallet_applications"
  private val applicationId = 6
  private val applicationUuid = UUID.randomUUID()

  implicit val ec = ExecutionContext.global

  "Wallet application API" should {
    "[non-wallet api] create application" in {
      val dbApi = inject[DBApi]
      val db = dbApi.database("backoffice")
      val insertQuery = SQL("INSERT INTO user_applications(id, uuid, user_id, status, stage, created_by, created_at, updated_at) " +
        s"VALUES($applicationId, '${applicationUuid.toString}', 4, 'pending', 'new', 'Admin', '2019-01-01 10:30:00', '2019-01-01 10:30:00');")

      db.withTransaction { implicit connection ⇒
        insertQuery.execute()
      }
      succeed
    }

    "get wallet application by uuid" in {
      val request = FakeRequest("GET", s"$basePath/$applicationUuid").withHeaders(AuthHeader)
      val resp = route(app, request).get
      status(resp) mustBe OK

      val walletApplicationOrError = contentAsString(resp).as(classOf[WalletApplicationDetail])
      val walletApplication = walletApplicationOrError.get

      walletApplication.id mustBe applicationUuid.toString
    }

    "return 404 NotFound('some message') if application id cannot be found" in {
      val notFoundEntityErrorId = UUID.randomUUID()
      val request = FakeRequest("GET", s"$basePath/$notFoundEntityErrorId").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedJson =
        s"""
           |"code":"NotFoundEntity",
           |"msg":"wallet application with id $notFoundEntityErrorId not found"
           |""".stripMargin.replaceAll("\n", "")

      status(resp) mustBe NOT_FOUND
      contentAsString(resp).toJsonStr.contains(expectedJson)
    }

    "return 400 Bad request if application id can not parsed to UUID" in {
      val request = FakeRequest("GET", s"$basePath/123231").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |"code": "Unknown",
           |	"msg": "Cannot parse parameter id as UUID: Invalid UUID string: 123231"
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains expectedResponse
    }

    "get all wallet applications from a given first name and last name criteria" in {
      val request = FakeRequest("GET", s"$basePath?name=marcos&full_name=Test&order_by=-status").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |{"total":2,
           |"results":[
           |{"id":"1482c1a0-7e70-4a0f-adef-a07caa3b3acb",
           |"customer_id":"910f02a0-48ef-418d-ac0a-06eff7ac9c90",
           |"full_name":"Test",
           |"person_id":null,
           |"msisdn":"+971544451674",
           |"status":"rejected",
           |"application_stage":"new",
           |"applied_at":"2019-01-30T00:00:00Z",
           |"checked_at":null,
           |"checked_by":null,
           |"reason_if_rejected":"test",
           |"total_score":null,
           |"updated_at":"2019-01-30T00:00:00Z"},
           |{"id":"cf057438-17c6-49ad-b0ba-8593c8951364",
           |"customer_id":"efe3b069-476e-4e36-8d22-53176438f55f",
           |"full_name":"Test",
           |"person_id":null,
           |"msisdn":"+97123456789",
           |"status":"approved",
           |"application_stage":"ocr",
           |"applied_at":"2019-01-30T00:00:00Z",
           |"checked_at":null,"checked_by":null,
           |"reason_if_rejected":null,
           |"total_score":null,
           |"updated_at":"2019-01-30T00:00:00Z"}],
           |"limit":null,
           |"offset":null
           |}
       """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResponse
    }

    "get all pending wallet applications" in {
      val request = FakeRequest("GET", s"$basePath?status=pending").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           {
           |	"total": 1,
           |	"results": [{
           |		"id": "${applicationUuid.toString}",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |    "full_name":null,
           |    "person_id":null,
           |		"msisdn": "+971522106589",
           |		"status": "pending",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-01T10:30:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null,
           |    "total_score":null,
           |    "updated_at":"2019-01-01T10:30:00Z"
           |	}],
           |	"limit": null,
           |	"offset": null
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResponse
    }

    "get paginated wallet applications if limit and offset query param is given" in {
      val request = FakeRequest("GET", s"$basePath?status=pending&limit=1&offset=0").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           {
           |	"total": 1,
           |	"results": [{
           |		"id": "${applicationUuid.toString}",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |    "full_name":null,
           |    "person_id":null,
           |		"msisdn": "+971522106589",
           |		"status": "pending",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-01T10:30:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null,
           |    "total_score":null,
           |    "updated_at":"2019-01-01T10:30:00Z"
           |	}],
           |	"limit": 1,
           |	"offset": 0
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedResponse
    }

    "get wallet applications withing specific date range" in {
      val startDate = LocalDate.of(2019, 1, 1)
      val endDate = LocalDate.of(2019, 1, 30)
      val request = FakeRequest("GET", s"$basePath?start_date=$startDate&end_date=$endDate").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |{
           |	"total": 5,
           |	"results": [{
           |		"id": "c0269418-3dd9-41fe-a756-d4b8ee6ff1c0",
           |		"customer_id": "0b507259-2c3a-48fd-97dc-3760a3756e6d",
           |		"msisdn": "+971507472520",
           |		"status": "rejected",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": "test"
           |	}, {
           |		"id": "1482c1a0-7e70-4a0f-adef-a07caa3b3acb",
           |		"customer_id": "910f02a0-48ef-418d-ac0a-06eff7ac9c90",
           |		"msisdn": "+971544451674",
           |		"status": "rejected",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": "test"
           |	}, {
           |		"id": "${applicationUuid.toString}",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"msisdn": "+971522106589",
           |		"status": "pending",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-01T10:30:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}, {
           |		"id": "cf057438-17c6-49ad-b0ba-8593c8951364",
           |		"customer_id": "efe3b069-476e-4e36-8d22-53176438f55f",
           |		"msisdn": "+97123456789",
           |		"status": "approved",
           |		"application_stage": "ocr",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}, {
           |		"id": "ac0718ad-3959-4327-a720-a47b590a2066",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"msisdn": "+971522106589",
           |		"status": "approved",
           |		"application_stage": "ocr",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}],
           |	"limit": null,
           |	"offset": null
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) contains expectedResponse
    }

    "get wallet applications from a given msisdn" in {
      val request = FakeRequest("GET", s"$basePath?msisdn=971522106589").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |{
           |	"total": 2,
           |	"results": [{
           |		"id": "${applicationUuid.toString}",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"msisdn": "+971522106589",
           |		"status": "pending",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-01T10:30:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}, {
           |		"id": "ac0718ad-3959-4327-a720-a47b590a2066",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"msisdn": "+971522106589",
           |		"status": "approved",
           |		"application_stage": "ocr",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}],
           |	"limit": null,
           |	"offset": null
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) contains expectedResponse
    }

    "get wallet applications from a name" in {
      val request = FakeRequest("GET", s"$basePath?name=Dave").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |{
           |	"total": 2,
           |	"results": [{
           |		"id": "${applicationUuid.toString}",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"msisdn": "+971522106589",
           |		"status": "pending",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-01T10:30:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}, {
           |		"id": "ac0718ad-3959-4327-a720-a47b590a2066",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"msisdn": "+971522106589",
           |		"status": "approved",
           |		"application_stage": "ocr",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null
           |	}],
           |	"limit": null,
           |	"offset": null
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) contains expectedResponse
    }


    "get wallet applications from a full name and national id" in {
      val request = FakeRequest("GET", s"$basePath?full_name=Test&national_id=A1").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           {
           |	"total": 5,
           |	"results": [{
           |		"id": "ac0718ad-3959-4327-a720-a47b590a2066",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"full_name": null,
           |		"person_id": null,
           |		"msisdn": "+971522106589",
           |		"status": "approved",
           |		"application_stage": "ocr",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null,
           |		"total_score": null
           |	}, {
           |		"id": "c0269418-3dd9-41fe-a756-d4b8ee6ff1c0",
           |		"customer_id": "0b507259-2c3a-48fd-97dc-3760a3756e6d",
           |		"full_name": null,
           |		"person_id": null,
           |		"msisdn": "+971507472520",
           |		"status": "rejected",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": "test",
           |		"total_score": null
           |	}, {
           |		"id": "f88b7179-b66f-4f54-b322-a13891346078",
           |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
           |		"full_name": null,
           |		"person_id": null,
           |		"msisdn": "+971522106589",
           |		"status": "pending",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-01T10:30:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null,
           |		"total_score": null
           |	}, {
           |		"id": "cf057438-17c6-49ad-b0ba-8593c8951364",
           |		"customer_id": "efe3b069-476e-4e36-8d22-53176438f55f",
           |		"full_name": null,
           |		"person_id": null,
           |		"msisdn": "+97123456789",
           |		"status": "approved",
           |		"application_stage": "ocr",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": null,
           |		"total_score": null
           |	}, {
           |		"id": "1482c1a0-7e70-4a0f-adef-a07caa3b3acb",
           |		"customer_id": "910f02a0-48ef-418d-ac0a-06eff7ac9c90",
           |		"full_name": null,
           |		"person_id": null,
           |		"msisdn": "+971544451674",
           |		"status": "rejected",
           |		"application_stage": "new",
           |		"applied_at": "2019-01-30T00:00:00Z",
           |		"checked_at": null,
           |		"checked_by": null,
           |		"reason_if_rejected": "test",
           |		"total_score": null
           |	}],
           |	"limit": null,
           |	"offset": null
           |}
             """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) contains expectedResponse
    }

    "get wallet applications from a national id" in {
     val request = FakeRequest("GET", s"$basePath?national_id=A1").withHeaders(AuthHeader)
     val resp = route(app, request).get

     val expectedResponse =
       s"""
          |{
          |	"total": 5,
          |	"results": [{
          |		"id": "f603087a-623e-4dd7-8ec7-adcd268844ec",
          |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
          |		"full_name": null,
          |		"person_id": null,
          |		"msisdn": "+971522106589",
          |		"status": "pending",
          |		"application_stage": "new",
          |		"applied_at": "2019-01-01T10:30:00Z",
          |		"checked_at": null,
          |		"checked_by": null,
          |		"reason_if_rejected": null,
          |		"total_score": null
          |	}, {
          |		"id": "ac0718ad-3959-4327-a720-a47b590a2066",
          |		"customer_id": "aaefd5fe-2e8b-4c8c-90f1-6ee9acaea53d",
          |		"full_name": null,
          |		"person_id": null,
          |		"msisdn": "+971522106589",
          |		"status": "approved",
          |		"application_stage": "ocr",
          |		"applied_at": "2019-01-30T00:00:00Z",
          |		"checked_at": null,
          |		"checked_by": null,
          |		"reason_if_rejected": null,
          |		"total_score": null
          |	}, {
          |		"id": "c0269418-3dd9-41fe-a756-d4b8ee6ff1c0",
          |		"customer_id": "0b507259-2c3a-48fd-97dc-3760a3756e6d",
          |		"full_name": null,
          |		"person_id": null,
          |		"msisdn": "+971507472520",
          |		"status": "rejected",
          |		"application_stage": "new",
          |		"applied_at": "2019-01-30T00:00:00Z",
          |		"checked_at": null,
          |		"checked_by": null,
          |		"reason_if_rejected": "test",
          |		"total_score": null
          |	}, {
          |		"id": "cf057438-17c6-49ad-b0ba-8593c8951364",
          |		"customer_id": "efe3b069-476e-4e36-8d22-53176438f55f",
          |		"full_name": null,
          |		"person_id": null,
          |		"msisdn": "+97123456789",
          |		"status": "approved",
          |		"application_stage": "ocr",
          |		"applied_at": "2019-01-30T00:00:00Z",
          |		"checked_at": null,
          |		"checked_by": null,
          |		"reason_if_rejected": null,
          |		"total_score": null
          |	}, {
          |		"id": "1482c1a0-7e70-4a0f-adef-a07caa3b3acb",
          |		"customer_id": "910f02a0-48ef-418d-ac0a-06eff7ac9c90",
          |		"full_name": null,
          |		"person_id": null,
          |		"msisdn": "+971544451674",
          |		"status": "rejected",
          |		"application_stage": "new",
          |		"applied_at": "2019-01-30T00:00:00Z",
          |		"checked_at": null,
          |		"checked_by": null,
          |		"reason_if_rejected": "test",
          |		"total_score": null
          |	}],
          |	"limit": null,
          |	"offset": null
          |}
        """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

     status(resp) mustBe OK
     contentAsString(resp) contains expectedResponse
   }

    "get wallet applications empty result when no criteria is matched" in {
      val request = FakeRequest("GET", s"$basePath?msisdn=971522106581").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |{
           |	"total": 0,
           |	"results": [],
           |	"limit": null,
           |	"offset": null
           |}
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe OK
      contentAsString(resp) contains expectedResponse
    }

    "return 400 BadRequest('some message') if given msisdn is not msisdn format" in {
      val request = FakeRequest("GET", s"$basePath?msisdn=9715221065131289").withHeaders(AuthHeader)
      val resp = route(app, request).get

      val expectedResponse =
        s"""
           |"code":"MalformedRequest",
           |"msg":"assertion failed: invalid Msisdn: 1231"
         """.stripMargin.replaceAll("\n", "").replaceAll("\\s", "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains expectedResponse
    }
  }
}
