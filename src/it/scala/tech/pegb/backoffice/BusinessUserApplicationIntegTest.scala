package tech.pegb.backoffice

import java.time.LocalDateTime
import java.util.UUID

import anorm.{Row, SQL, SqlParser}
import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.{DBApi, Database}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.businessuserapplication.controllers.BusinessUserApplicationController
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.dao.businessuserapplication.sql.{BUApplicPrimaryAddressesSqlDao, BUApplicPrimaryContactsSqlDao, BusinessUserApplicationConfigSqlDao, BusinessUserApplicationSqlDao}
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.{Stages, Status}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.TestExecutionContext

import scala.concurrent.Future

class BusinessUserApplicationIntegTest extends PlayIntegrationTest with MockFactory with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))
  val httpClientService = stub[HttpClient]

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  //NOTE: for some BS reason, the foreign key from business_user_application to business_user_application_primary_contacts
  //through default_contact_id is still existing though it has been dropped, thus the SET FOREIGN_KEY_CHECKS = 0
  override def cleanupSql: String =
    s"""
       |SET FOREIGN_KEY_CHECKS = 0;
       |DELETE FROM business_user_application_primary_addresses;
       |DELETE FROM business_user_application_primary_contacts;
       |DELETE FROM business_user_application_account_configs;
       |DELETE FROM business_user_application_external_accounts;
       |DELETE FROM business_user_applications;
       |SET FOREIGN_KEY_CHECKS = 1;
     """.stripMargin

  val dbNow = now.toZonedDateTimeUTC.toLocalDateTimeUTC

  override def initSql =
    s"""
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('1','fcad736b-a6d8-4b8e-845d-edb83489ac50','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','$dbNow','$dbNow'),
       |('2','926551f7-84f6-459e-bf15-a76d07d1b712','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','$dbNow','$dbNow'),
       |('3','0e602b13-7283-4424-a683-d9b58cddadb3','Henry Sy and kids','SM Megamall','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '999/212EE', 'C12342M', '1999-01-01','system','system','$dbNow','$dbNow'),
       |('4','572c13a1-8e2f-491e-90c7-1e328cf2c094','Ayala family','Greenbelt','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '888/212EE', 'D12342M', '2005-01-01','system','system','$dbNow','$dbNow');
       |""".stripMargin


  override val endpoint = s"/api/${inject[BusinessUserApplicationController].getRoute}"

  //TODO: remove binding when erland endpoint is ready
  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[HttpClient].to(httpClientService),
      //bind[DocumentImmutableFileDao].to(documentImmutableFileDao),
      //bind[DocumentTransientFileDao].to(documentTransientFileDao),
      bind[WithExecutionContexts].to(TestExecutionContext)
    )

  val db: Database = inject[DBApi].database("backoffice")
  val config = inject[AppConfig]

  "BusinessUserApplication api" should {

    "GET all business_user_applications in GET /api/business_user_applications" in {
      val expected =
        s"""
          |{
          |"total":4,
          |"results":[{
          |"id":"572c13a1-8e2f-491e-90c7-1e328cf2c094",
          |"business_name":"Ayala family",
          |"brand_name":"Greenbelt",
          |"business_category":"Department Store - 9999",
          |"stage":"application_documents",
          |"status":"pending",
          |"user_tier":"basic",
          |"business_type":"merchant",
          |"registration_number":"888/212EE",
          |"tax_number":"D12342M",
          |"registration_date":"2005-01-01",
          |"explanation":null,
          |"submitted_by":null,
          |"submitted_at":null,
          |"checked_by":null,
          |"checked_at":null,
          |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"AED"}],
          |"created_by":"system",
          |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
          |"updated_by":"system",
          |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}},
          |{
          |"id":"0e602b13-7283-4424-a683-d9b58cddadb3",
          |"business_name":"Henry Sy and kids",
          |"brand_name":"SM Megamall",
          |"business_category":"Department Store - 9999",
          |"stage":"application_documents",
          |"status":"pending",
          |"user_tier":"basic",
          |"business_type":"merchant",
          |"registration_number":"999/212EE",
          |"tax_number":"C12342M",
          |"registration_date":"1999-01-01",
          |"explanation":null,
          |"submitted_by":null,
          |"submitted_at":null,
          |"checked_by":null,
          |"checked_at":null,
          |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"AED"}],
          |"created_by":"system",
          |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
          |"updated_by":"system",
          |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}},
          |{
          |"id":"926551f7-84f6-459e-bf15-a76d07d1b712",
          |"business_name":"Tindahan ni Aling Nena",
          |"brand_name":"Nenas Store",
          |"business_category":"Store - 1111",
          |"stage":"application_documents",
          |"status":"approved",
          |"user_tier":"basic",
          |"business_type":"merchant",
          |"registration_number":"111/212EE",
          |"tax_number":"A12342M",
          |"registration_date":"1990-01-01",
          |"explanation":null,
          |"submitted_by":null,
          |"submitted_at":null,
          |"checked_by":null,
          |"checked_at":null,
          |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"AED"}],
          |"created_by":"system",
          |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
          |"updated_by":"system",
          |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}
          |},
          |{
          |"id":"fcad736b-a6d8-4b8e-845d-edb83489ac50",
          |"business_name":"Universal Catering Co",
          |"brand_name":"Costa Coffee DSO",
          |"business_category":"Restaurants - 5182",
          |"stage":"identity_info",
          |"status":"ongoing",
          |"user_tier":"basic",
          |"business_type":"merchant",
          |"registration_number":"212/212EE",
          |"tax_number":"B12342M",
          |"registration_date":"2019-01-01",
          |"explanation":null,
          |"submitted_by":null,
          |"submitted_at":null,
          |"checked_by":null,
          |"checked_at":null,
          |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"AED"}],
          |"created_by":"system",
          |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
          |"updated_by":"system",
          |"updated_at":${dbNow.toZonedDateTimeUTC.toJsonStr}
          |}],
          |"limit":null,
          |"offset":null}""".stripMargin.replace(System.lineSeparator(), "")

      //get
      val getRequest = FakeRequest(GET, s"$endpoint?order_by=business_name").withHeaders(AuthHeader)
      val resp = route(app, getRequest).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "create business user application in POST /business_user_applications" in {
      val uuid = UUID.randomUUID()

      val jsonRequest =
        s"""{
           |  "business_name": "Ayala Malls",
           |  "brand_name": "Glorietta 4",
           |  "business_category": "Department store",
           |  "user_tier": "big",
           |  "business_type": "merchant",
           |  "registration_number": "12345678qwe",
           |  "tax_number": "qwe12345678",
           |  "registration_date": "1990-08-24"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"$endpoint/$uuid", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""{
           |"id":"${uuid}",
           |"business_name":"Ayala Malls",
           |"brand_name":"Glorietta 4",
           |"business_category":"Department store",
           |"stage":"identity_info",
           |"status":"ongoing",
           |"user_tier":"big",
           |"business_type":"merchant",
           |"registration_number":"12345678qwe",
           |"tax_number":"qwe12345678",
           |"registration_date":"1990-08-24",
           |"explanation":null,
           |"submitted_by":null,
           |"submitted_at":null,
           |"checked_by":null,
           |"checked_at":null,
           |"valid_transaction_config":[{"transaction_type":"merchant_payment","currency_code":"AED"}],
           |"created_by":"superuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"superuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "create config in POST /api/business_user_applications/:id/stage/config" in {
      val buApplicId = "fcad736b-a6d8-4b8e-845d-edb83489ac50"

      val jsonPayload =
        s"""
           |{
           |  "transaction_config": [
           |    {
           |      "transaction_type": "merchant_payment",
           |      "currency_code": "AED"
           |    }
           |  ],
           |  "account_config": [
           |    {
           |      "account_type": "collection",
           |      "account_name": "Default Collection",
           |      "currency_code": "AED",
           |      "is_default": true
           |    }
           |  ],
           |  "external_accounts": [
           |    {
           |      "provider": "mPesa",
           |      "account_number": "955100",
           |      "account_holder": "Costa Coffee FZE",
           |      "currency_code": "AED"
           |    }
           |  ]
           |}
         """.stripMargin

      val expected =
        s"""
           |{
           |"id":"fcad736b-a6d8-4b8e-845d-edb83489ac50",
           |"status":"ongoing",
           |"transaction_config":[{"transaction_type":"merchant_payment","currency_code":"AED"}],
           |"account_config":[{"account_type":"collection","account_name":"Default Collection","currency_code":"AED","is_default":true}],
           |"external_accounts":[{"provider":"mPesa","account_number":"955100","account_holder":"Costa Coffee FZE","currency_code":"AED"}],
           |"created_by":"system",
           |"created_at":${dbNow.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"superuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"submitted_by":null}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest("POST", s"$endpoint/$buApplicId/stage/${Stages.Config}")
        .withBody(jsonPayload)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected

      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val query1 = SQL(s"SELECT id FROM ${BusinessUserApplicationConfigSqlDao.TransactionConfigMeta.tableName} WHERE application_id = 1")
        val txnFound: Option[Row] = query1.executeQuery().as(query1.defaultParser.singleOpt)

        val query2 = SQL(s"SELECT id FROM ${BusinessUserApplicationConfigSqlDao.AccountConfigMeta.tableName} WHERE application_id = 1")
        val accountFound: Option[Row] = query2.executeQuery().as(query2.defaultParser.singleOpt)

        val query3 = SQL(s"SELECT id FROM ${BusinessUserApplicationConfigSqlDao.ExternalAccountMeta.tableName} WHERE application_id = 1")
        val externalAccountFound: Option[Row] = query2.executeQuery().as(query3.defaultParser.singleOpt)


        txnFound.isDefined && accountFound.isDefined && externalAccountFound.isDefined
      }
      isReallyInDB mustBe true
    }

    "create primary contacts and addresses in POST /api/business_user_applications/:id/stage/contact_info" in {
      val buApplicId = "fcad736b-a6d8-4b8e-845d-edb83489ac50"

      val jsonPayload =
        s"""
           |{
           |"contacts":[
           |{"contact_type":"business_owner",
           |"name":"Lloyd",
           |"middle_name":"Pepito",
           |"surname":"Edano",
           |"phone_number":"+971544451679",
           |"email":"o.lloyd@pegb.tech",
           |"id_type":"National ID",
           |"is_velocity_user":true,
           |"velocity_level":"admin"}
           |],
           |"addresses":[
           |{"address_type":"primary_address",
           |"country":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"22B Street Muraqabat Road Bafta Building Flat 102",
           |"coordinate_x":36.12334,
           |"coordinate_y":128.34566}
           |],
           |"updated_at":null
           |}
         """.stripMargin

      val resp = route(app, FakeRequest("POST", s"$endpoint/$buApplicId/stage/${Stages.Contact}")
        .withBody(jsonPayload)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val (createdAtFromDB, createdByFromDB, updatedAtFromDB, updatedByFromDB) = db.withConnection { implicit conn⇒
        val query = SQL(s"SELECT * FROM ${BusinessUserApplicationSqlDao.TableName} WHERE uuid = '$buApplicId'")
        query.executeQuery().as(query.defaultParser.singleOpt)
          .map(row⇒
            (
              row[LocalDateTime]("created_at"),
              row[String]("created_by"),
              row[LocalDateTime]("updated_at"),
              row[String]("updated_by"),
            )).get
      }

      val expected =
        s"""
           |{
           |"id":"$buApplicId",
           |"status":"ongoing",
           |"contacts":[
           |{"contact_type":"business_owner",
           |"name":"Lloyd",
           |"middle_name":"Pepito",
           |"surname":"Edano",
           |"phone_number":"+971544451679",
           |"email":"o.lloyd@pegb.tech",
           |"id_type":"National ID",
           |"is_velocity_user":true,
           |"velocity_level":"admin",
           |"is_default_contact":true}
           |],
           |"addresses":[
           |{"address_type":"primary_address",
           |"country":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"22B Street Muraqabat Road Bafta Building Flat 102",
           |"coordinate_x":36.12334,
           |"coordinate_y":128.34566}
           |],
           |"created_at":${createdAtFromDB.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"${createdByFromDB}",
           |"updated_at":${updatedAtFromDB.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"${updatedByFromDB}",
           |"submitted_by":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expected

      val isReallyInDB: Boolean = db.withConnection { implicit conn⇒
        val query1 = SQL(s"SELECT name FROM ${BUApplicPrimaryContactsSqlDao.TableName} WHERE name = 'Lloyd'")
        val contactFound: Option[Row] = query1.executeQuery().as(query1.defaultParser.singleOpt)

        val query2 = SQL(s"SELECT id FROM ${BUApplicPrimaryAddressesSqlDao.TableName} WHERE country_id = 1")
        val addressFound: Option[Row] = query2.executeQuery().as(query2.defaultParser.singleOpt)

        contactFound.isDefined && addressFound.isDefined
      }
      isReallyInDB mustBe true
      
    }

    "get primary contacts and addresses in GET /api/business_user_applications/:id/stage/contact_info" in {
      val buApplicId = "fcad736b-a6d8-4b8e-845d-edb83489ac50"
      val resp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId/stage/${Stages.Contact}")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val (createdAtFromDB, createdByFromDB, updatedAtFromDB, updatedByFromDB) = db.withConnection { implicit conn⇒
        val query = SQL(s"SELECT * FROM ${BusinessUserApplicationSqlDao.TableName} WHERE uuid = '$buApplicId'")
        query.executeQuery().as(query.defaultParser.singleOpt)
          .map(row⇒
            (
              row[LocalDateTime]("created_at"),
              row[String]("created_by"),
              row[LocalDateTime]("updated_at"),
              row[String]("updated_by"),
            )).get
      }

      val expectedJson =
        s"""
           |{
           |"id":"$buApplicId",
           |"status":"ongoing",
           |"contacts":[
           |{"contact_type":"business_owner",
           |"name":"Lloyd",
           |"middle_name":"Pepito",
           |"surname":"Edano",
           |"phone_number":"+971544451679",
           |"email":"o.lloyd@pegb.tech",
           |"id_type":"National ID",
           |"is_velocity_user":true,
           |"velocity_level":"admin",
           |"is_default_contact":true}
           |],
           |"addresses":[
           |{"address_type":"primary_address",
           |"country":"UAE",
           |"city":"Dubai",
           |"postal_code":"00000",
           |"address":"22B Street Muraqabat Road Bafta Building Flat 102",
           |"coordinate_x":36.12334,
           |"coordinate_y":128.34566}
           |],
           |"created_at":${createdAtFromDB.toZonedDateTimeUTC.toJsonStr},
           |"created_by":"${createdByFromDB}",
           |"updated_at":${updatedAtFromDB.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"${updatedByFromDB}",
           |"submitted_by":null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson

    }

    "submit application using PUT /api/business_user_applications/:id/submit" in {
      val buApplicId = "fcad736b-a6d8-4b8e-845d-edb83489ac50"

      val getResp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId")
        .withHeaders(jsonHeaders)).get

      val getResponseJson = contentAsJson(getResp)
      val lastUpdated = (getResponseJson \ "updated_at").as[String]

      val jsonRequest =
        s"""
          |{
          |"updated_at":${lastUpdated.toJsonStr}
          |}""".stripMargin

      val resp = route(app, FakeRequest("PUT", s"$endpoint/$buApplicId/submit")
        .withBody(jsonRequest)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val (bStatus, bSubmittedBy): (Option[String], Option[String]) = db.withConnection { implicit conn⇒
        val query1 = SQL(s"SELECT * FROM ${BusinessUserApplicationSqlDao.TableName} WHERE uuid = '$buApplicId'")
        val status = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cStatus).singleOpt)
        val submittedBy = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cSubmittedBy).singleOpt)

        (status, submittedBy)
      }

      bStatus mustBe Some(Status.Pending)
      bSubmittedBy mustBe Some("superuser")

    }

    "send application for correction using PUT /api/business_user_applications/:id/send_for_correction" in {
      val buApplicId = "fcad736b-a6d8-4b8e-845d-edb83489ac50"

      val getResp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId")
        .withHeaders(jsonHeaders)).get

      val getResponseJson = contentAsJson(getResp)
      val lastUpdated = (getResponseJson \ "updated_at").as[String]

      val jsonRequest =
        s"""
           |{
           |"explanation":"Send back for correction",
           |"updated_at":${lastUpdated.toJsonStr}
           |}""".stripMargin

      val resp = route(app, FakeRequest("PUT", s"$endpoint/$buApplicId/send_for_correction")
        .withBody(jsonRequest)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val (bStatus, bExplanation): (Option[String], Option[String]) = db.withConnection { implicit conn⇒
        val query1 = SQL(s"SELECT * FROM ${BusinessUserApplicationSqlDao.TableName} WHERE uuid = '$buApplicId'")
        val status = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cStatus).singleOpt)
        val explanation = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cExplanation).singleOpt)

        (status, explanation)
      }

      bStatus mustBe Some(Status.Ongoing)
      bExplanation mustBe Some("Send back for correction")

    }

    "cancel application for correction using PUT /api/business_user_applications/:id/cancel" in {
      val buApplicId = "fcad736b-a6d8-4b8e-845d-edb83489ac50"

      val getResp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId")
        .withHeaders(jsonHeaders)).get

      val getResponseJson = contentAsJson(getResp)
      val lastUpdated = (getResponseJson \ "updated_at").as[String]

      val jsonRequest =
        s"""
           |{
           |"explanation":"Cancelling application",
           |"updated_at":${lastUpdated.toJsonStr}
           |}""".stripMargin

      val resp = route(app, FakeRequest("PUT", s"$endpoint/$buApplicId/cancel")
        .withBody(jsonRequest)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val (bStatus, bExplanation): (Option[String], Option[String]) = db.withConnection { implicit conn⇒
        val query1 = SQL(s"SELECT * FROM ${BusinessUserApplicationSqlDao.TableName} WHERE uuid = '$buApplicId'")
        val status = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cStatus).singleOpt)
        val explanation = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cExplanation).singleOpt)

        (status, explanation)
      }

      bStatus mustBe Some(Status.Cancelled)
      bExplanation mustBe Some("Cancelling application")

    }

    "reject application using PUT /api/business_user_applications/:id/reject" in {
      val buApplicId = "0e602b13-7283-4424-a683-d9b58cddadb3"

      val getResp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId")
        .withHeaders(jsonHeaders)).get

      val getResponseJson = contentAsJson(getResp)
      val lastUpdated = (getResponseJson \ "updated_at").as[String]

      val jsonRequest =
        s"""
           |{
           |"explanation":"Rejecting this",
           |"updated_at":${lastUpdated.toJsonStr}
           |}""".stripMargin

      val resp = route(app, FakeRequest("PUT", s"$endpoint/$buApplicId/reject")
        .withBody(jsonRequest)
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK

      val (bStatus, bExplanation, bCheckedBy): (Option[String], Option[String], Option[String]) = db.withConnection { implicit conn⇒
        val query1 = SQL(s"SELECT * FROM ${BusinessUserApplicationSqlDao.TableName} WHERE uuid = '$buApplicId'")
        val status = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cStatus).singleOpt)
        val explanation = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cExplanation).singleOpt)
        val checkedBy = query1.executeQuery().as(SqlParser.str(BusinessUserApplicationSqlDao.cCheckedBy).singleOpt)

        (status, explanation, checkedBy)
      }

      bStatus mustBe Some(Status.Rejected)
      bExplanation mustBe Some("Rejecting this")
      bCheckedBy mustBe Some("superuser")

    }
  }


  "approve application using PUT /api/business_user_applications/:id/approve" in {
    val buApplicId = "572c13a1-8e2f-491e-90c7-1e328cf2c094"
    val fakeBuId = 1

    val getResp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId")
      .withHeaders(jsonHeaders)).get

    val getResponseJson = contentAsJson(getResp)
    val lastUpdated = (getResponseJson \ "updated_at").as[String]

    val jsonRequest =
      s"""
         |{
         |"updated_at":${lastUpdated.toJsonStr}
         |}""".stripMargin


    val coreResponse =
      s"""
        |{
        |  "id" : 55,
        |  "uuid" : "1ea7377c-8224-41ef-9719-b084236f3b66",
        |  "user_id" : $fakeBuId,
        |  "business_name" : "Ayala family",
        |  "brand_name" : "Greenbelt",
        |  "business_category" :  "Department Store - 9999",
        |  "business_type" :  "merchant",
        |  "registration_number" : "888/212EE",
        |  "tax_number" : "D12342M",
        |  "registration_date" : "2005-01-01",
        |  "created_by" : "superuser",
        |  "updated_by" : "superuser",
        |  "created_at" : "$dbNow",
        |  "updated_at" : "$dbNow"
        |}
      """.stripMargin
    (httpClientService.request (_: String, _: String, _: Option[JsValue]))
      .when("POST", s"${config.CreateBusinessUserUrl}", Json.obj("application_id" → 4, "created_by" → "superuser").some)
      .returns(Future.successful(HttpResponse(true, 204, coreResponse.some)))

    val resp = route(app, FakeRequest("PUT", s"$endpoint/$buApplicId/approve")
      .withBody(jsonRequest)
      .withHeaders(jsonHeaders)).get

    status(resp) mustBe OK

  }

  "return 408 in approve application when timeout encountered in core using PUT /api/business_user_applications/:id/approve" in {
    val buApplicId = "572c13a1-8e2f-491e-90c7-1e328cf2c094"
    val fakeBuId = 1

    val getResp = route(app, FakeRequest("GET", s"$endpoint/$buApplicId")
      .withHeaders(jsonHeaders)).get

    val getResponseJson = contentAsJson(getResp)
    val lastUpdated = (getResponseJson \ "updated_at").as[String]

    val jsonRequest =
      s"""
         |{
         |"updated_at":${lastUpdated.toJsonStr}
         |}""".stripMargin


    val coreResponse =
      s"""
         |{
         |  "error": "Timeout encountered on create Business User Application"
         |}
      """.stripMargin
    (httpClientService.request (_: String, _: String, _: Option[JsValue]))
      .when("POST", s"${config.CreateBusinessUserUrl}", Json.obj("application_id" → 4, "created_by" → "superuser").some)
      .returns(Future.successful(HttpResponse(false, 408, coreResponse.some)))

    val resp = route(app, FakeRequest("PUT", s"$endpoint/$buApplicId/approve")
      .withBody(jsonRequest)
      .withHeaders(jsonHeaders)).get

    status(resp) mustBe REQUEST_TIMEOUT
    (contentAsJson(resp) \ "msg").get.toString should include("Timeout encountered on calling CORE API. Please try again after 5 minutes: 408")

  }

}
