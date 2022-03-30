package it.scala.tech.pegb.backoffice

import java.util.UUID

import anorm._
import org.scalatest.concurrent.ScalaFutures
import play.api.db.{DBApi, Database}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.PlayIntegrationTest
import tech.pegb.backoffice.dao.account.sql.ExternalAccountSqlDao

//Rename me
class ExternalAccountMgmtIntegTest extends PlayIntegrationTest with ScalaFutures {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  override def cleanupSql =
    s"""
       |DELETE FROM business_user_external_accounts;
       |DELETE FROM business_users WHERE uuid IN ('b713452a-3349-41f7-a2f5-a7cf23b8d1ce', 'b084d14a-ad90-4170-b574-a400947c135d');
       |DELETE FROM users WHERE uuid IN ('b713452a-3349-41f7-a2f5-a7cf23b8d1ce', 'b084d14a-ad90-4170-b574-a400947c135d');
     """.stripMargin

  override def initSql =
    s"""
       |$cleanupSql
       |
       |INSERT INTO `users` (`uuid`,`username`,`password`,`type`,`tier`,`segment`,`subscription`,`email`,`status`,`activated_at`,`password_updated_at`,`created_at`,`created_by`,`updated_at`,`updated_by`)
       |VALUES
       |('b713452a-3349-41f7-a2f5-a7cf23b8d1ce', 'Carrefour Deira City Center', 'password', 'business', 'big', 'new', 'standard', NULL, 'active', '2019-01-03 06:26:21', '2019-01-03 06:26:21', '2019-01-20 07:22:46', 'admin', '2019-01-03 06:26:21', NULL),
       |('b084d14a-ad90-4170-b574-a400947c135d', 'West Zone Al Rigga', 'password', 'business', 'big', 'new', 'standard', NULL, 'active', '2019-01-03 06:26:21', '2019-01-03 06:26:21', '2019-01-20 07:22:46', 'admin', '2019-01-03 06:26:21', NULL);
       |
       |INSERT INTO business_users
       |(uuid, user_id, business_name, brand_name, business_category, business_type, registration_number, tax_number, registration_date, currency_id, collection_account_id, distribution_account_id, created_at,created_by,updated_at,updated_by)
       |VALUES
       |('b713452a-3349-41f7-a2f5-a7cf23b8d1ce',  (SELECT id FROM users WHERE uuid = 'b713452a-3349-41f7-a2f5-a7cf23b8d1ce'),
       | 'Carrefour', 'Carrefour', 'Retail', 'Merchant', '123456', '098765', '2005-01-01', '1', null, null, '2019-01-20 07:22:46', 'admin', '2019-01-03 06:26:21', NULL),
       | ('b084d14a-ad90-4170-b574-a400947c135d',  (SELECT id FROM users WHERE uuid = 'b084d14a-ad90-4170-b574-a400947c135d'),
       | 'West Zone', 'West Zone', 'Retail', 'Merchant', '123456', '098765', '2005-01-01', '1', null, null, '2019-01-20 07:22:46', 'admin', '2019-01-03 06:26:21', NULL);
       |
       |INSERT INTO business_user_external_accounts
       |(id, uuid, user_id, provider, account_number, account_holder, currency_id, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', 'c23ae501-0144-4307-8bde-37ba41e9733f', (SELECT id FROM users WHERE uuid = 'b713452a-3349-41f7-a2f5-a7cf23b8d1ce'), 'Mashreq',   '0000-1111', 'Alex Kharlamov', 1, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00'),
       |('2', 'c5d870ad-940b-48c5-bea7-7cc9bd58c3ed', (SELECT id FROM users WHERE uuid = 'b713452a-3349-41f7-a2f5-a7cf23b8d1ce'), 'Noor Bank', '0000-2222', 'George Ogalo', 1, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00'),
       |('3', '9ecf62f8-fcd1-4f17-beed-fcdf3433caf0', (SELECT id FROM users WHERE uuid = 'b713452a-3349-41f7-a2f5-a7cf23b8d1ce'), 'RAK BANK', '0000-3333', 'Alex Kim', 1, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00'),
       |('4', '9cc96106-a793-4b09-a39c-42da4f8395ee', (SELECT id FROM users WHERE uuid = 'b713452a-3349-41f7-a2f5-a7cf23b8d1ce'), 'Dubai Islamic', '1111-0022', 'Alex Sverdlov', 2, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00'),
       |('5', '522687cd-9754-4dff-8902-cf0675757f6e', (SELECT id FROM users WHERE uuid = 'b084d14a-ad90-4170-b574-a400947c135d'), 'Emirated NBD', '2222-12345', 'Alex Kharlamov', 2, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00'),
       |('6', '0b62e441-1dbf-4e62-bd03-fa6ec6305b33', (SELECT id FROM users WHERE uuid = 'b084d14a-ad90-4170-b574-a400947c135d'), 'Mashreq', '2222-1111', 'George Washington', 3, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00'),
       |('7', '4034cd77-d855-45e4-b4b6-015d2955e306', (SELECT id FROM users WHERE uuid = 'b084d14a-ad90-4170-b574-a400947c135d'), 'Mashreq', '0000-3333', 'George Clooney', 3, 'test', '2020-01-05 00:00:00', 'test', '2020-01-05 00:00:00');
     """.stripMargin

  override val endpoint = "external_accounts"

  def parentEndpoint = "/api/business_users"

  val db: Database = inject[DBApi].database("backoffice")

  "External account management api" should {
    "fetch all external accounts of a chosen customer in GET /api/business_users/:id/external_accounts" in {
      val customerId = "b713452a-3349-41f7-a2f5-a7cf23b8d1ce"
      val resp = route(app, FakeRequest(GET, s"$parentEndpoint/$customerId/$endpoint").withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |{"total":4,
           |"results":[
           |{"id":"c23ae501-0144-4307-8bde-37ba41e9733f",
           |"customer_id":"b713452a-3349-41f7-a2f5-a7cf23b8d1ce",
           |"provider":"Mashreq","account_number":"0000-1111","account_holder":"Alex Kharlamov",
           |"currency":"AED","updated_at":"2020-01-05T00:00:00Z"},
           |
           |{"id":"c5d870ad-940b-48c5-bea7-7cc9bd58c3ed",
           |"customer_id":"b713452a-3349-41f7-a2f5-a7cf23b8d1ce",
           |"provider":"Noor Bank","account_number":"0000-2222","account_holder":"George Ogalo",
           |"currency":"AED","updated_at":"2020-01-05T00:00:00Z"},
           |
           |{"id":"9ecf62f8-fcd1-4f17-beed-fcdf3433caf0",
           |"customer_id":"b713452a-3349-41f7-a2f5-a7cf23b8d1ce",
           |"provider":"RAK BANK","account_number":"0000-3333","account_holder":"Alex Kim",
           |"currency":"AED","updated_at":"2020-01-05T00:00:00Z"},
           |
           |{"id":"9cc96106-a793-4b09-a39c-42da4f8395ee",
           |"customer_id":"b713452a-3349-41f7-a2f5-a7cf23b8d1ce",
           |"provider":"Dubai Islamic","account_number":"1111-0022","account_holder":"Alex Sverdlov",
           |"currency":"INR","updated_at":"2020-01-05T00:00:00Z"}],
           |
           |"limit":4,"offset":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
      //println(">>>>> " + contentAsString(resp))
      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT uuid FROM ${ExternalAccountSqlDao.TableName} WHERE user_id = (SELECT id FROM users WHERE uuid = '$customerId') ORDER BY id")
        val rows = result.executeQuery().as(result.defaultParser.*).map(r⇒ r[String]("uuid"))
        rows.size == 4 &&
        rows(0) == "c23ae501-0144-4307-8bde-37ba41e9733f" &&
          rows(1) == "c5d870ad-940b-48c5-bea7-7cc9bd58c3ed" &&
          rows(2) == "9ecf62f8-fcd1-4f17-beed-fcdf3433caf0" &&
          rows(3) == "9cc96106-a793-4b09-a39c-42da4f8395ee"
      }
      isReallyInDB mustBe true
    }

    "create new external account for chosen customer in POST /api/business_users/:id/external_accounts" in {
      val customerId = "b084d14a-ad90-4170-b574-a400947c135d"
      val jsonRequest =
        s"""
           |{
           |"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_holder":"Lloyd Edano",
           |"currency":"PHP"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$parentEndpoint/$customerId/$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |{"id":"${mockRequestId}",
           |"customer_id":"b084d14a-ad90-4170-b574-a400947c135d",
           |"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_holder":"Lloyd Edano",
           |"currency":"PHP",
           |"updated_at":null}
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      //println(">>>>" + contentAsString(resp))
      contentAsString(resp) mustBe expectedJson

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM ${ExternalAccountSqlDao.TableName} WHERE uuid = '${mockRequestId}'")
        val row = result.executeQuery().as(result.defaultParser.single)
        row[String]("uuid") == mockRequestId.toString &&
          row[String]("provider") == "Bank of the Philippine Islands" &&
          row[String]("account_number") == "019283726256389000013" &&
          row[String]("account_holder") == "Lloyd Edano"
      }
      isReallyInDB mustBe true
    }

    "fail to create duplicate external account (same customer_id, provider, account_number and account_holder)" in {
      val customerId = "b084d14a-ad90-4170-b574-a400947c135d"
      val jsonRequest =
        s"""
           |{
           |"provider":"Bank of the Philippine Islands",
           |"account_number":"019283726256389000013",
           |"account_holder":"Lloyd Edano",
           |"currency":"PHP"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$parentEndpoint/$customerId/$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get

      status(resp) mustBe CONFLICT

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM ${ExternalAccountSqlDao.TableName} WHERE provider = 'Bank of the Philippine Islands' and account_number = '019283726256389000013'")
        val row = result.executeQuery().as(result.defaultParser.single)
          row[String]("provider") == "Bank of the Philippine Islands" &&
          row[String]("account_number") == "019283726256389000013"
      }
      isReallyInDB mustBe true
    }

    "fail to create external account (customer_id not found)" in {
      val customerId = UUID.randomUUID()
      val jsonRequest =
        s"""
           |{
           |"provider":"BDO",
           |"account_number":"0192837262563891111108",
           |"account_holder":"Leo Edano",
           |"currency":"PHP"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$parentEndpoint/$customerId/$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get

      status(resp) mustBe NOT_FOUND

      val isReallyNotInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM users WHERE uuid = '$customerId'")
        val row = result.executeQuery().as(result.defaultParser.singleOpt)
        row == None
      }
      isReallyNotInDB mustBe true
    }

    "fail to create external account (currency invalid)" in {
      val customerId = "b084d14a-ad90-4170-b574-a400947c135d"
      val jsonRequest =
        s"""
           |{
           |"provider":"BDO",
           |"account_number":"0192837262563891111108",
           |"account_holder":"Leo Edano",
           |"currency":"XYZ"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$parentEndpoint/$customerId/$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get

      status(resp) mustBe BAD_REQUEST
    }

    "fail to create external account (currency not found)" in {
      val customerId = "b084d14a-ad90-4170-b574-a400947c135d"
      val jsonRequest =
        s"""
           |{
           |"provider":"BDO",
           |"account_number":"0192837262563891111108",
           |"account_holder":"Leo Edano",
           |"currency":"JPY"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(POST, s"$parentEndpoint/$customerId/$endpoint", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get

      status(resp) mustBe NOT_FOUND
    }

    "update external account in PUT /api/business_users/:id/external_accounts/:external_acc_id" in {
      val customerId = "b084d14a-ad90-4170-b574-a400947c135d"
      val extAccId = "522687cd-9754-4dff-8902-cf0675757f6e"
      val jsonRequest =
        s"""
           |{"provider":"Shiny Brand New Provider Co. Ltd.",
           |"updated_at":"2020-01-05T00:00:00Z"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(PUT, s"$parentEndpoint/$customerId/$endpoint/$extAccId", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get

      status(resp) mustBe OK

      val isReallyInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM ${ExternalAccountSqlDao.TableName} WHERE uuid = '$extAccId'")
        val row = result.executeQuery().as(result.defaultParser.single)
        row[String]("uuid") == extAccId &&
        row[String]("provider") == "Shiny Brand New Provider Co. Ltd."
      }
      isReallyInDB mustBe true
    }

    "expected behavior of DELETE endpoint" in {
      val customerId = "b084d14a-ad90-4170-b574-a400947c135d"
      val extAccId = "4034cd77-d855-45e4-b4b6-015d2955e306"
      val jsonRequest =
        s"""
           |{"updated_at":"2020-01-05T00:00:00Z"}
         """.stripMargin.replace(System.lineSeparator(), "")

      val resp = route(app, FakeRequest(DELETE, s"$parentEndpoint/$customerId/$endpoint/$extAccId", jsonHeaders, jsonRequest).withHeaders(AuthHeader)).get
      val expectedJson =
        s"""
           |
           |""".stripMargin.replace(System.lineSeparator(), "")

      status(resp) mustBe OK
      //contentAsString(resp) mustBe expectedJson

      val isReallyNotInDB = db.withConnection { implicit conn⇒
        val result = SQL(s"SELECT * FROM ${ExternalAccountSqlDao.TableName} WHERE uuid = '$extAccId'")
        val row = result.executeQuery().as(result.defaultParser.*)
        row.size == 0
      }
      isReallyNotInDB mustBe true
    }
  }
}
