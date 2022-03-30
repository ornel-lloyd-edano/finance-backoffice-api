package tech.pegb.backoffice

import org.scalatest.Matchers._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, route, status, _}

class MakerCheckerIntegrationTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  "MakerCheckerIntegrationTest API POSITIVE READ" should {
    val apiKeyFromTrustedCaller = conf.get[String]("api-keys.backoffice-auth-api")
    val requesterLevel = 2
    val requesterBusinessUnit = "BackOffice"

    "return success in detailed task in get by id " in {

      val resp = route(app, FakeRequest(GET, s"/tasks/06d18f41-1abf-4507-afab-5f8e1c7a1601")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"change":{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"},
           |"original_value":null,
           |"is_read_only":true,
           |"stale":false
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria without filter [CEO]" in {
      val requesterLevel = 0
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks?order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"total":6,
           |"results":[
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"ecb907ae-ffaa-45da-abd2-3907fced637f",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T05:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T05:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"54403083-e0f1-4e80-bdd6-cfdaefd0646e",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"approved",
           |"reason":null,
           |"created_at":"2019-01-02T11:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:20:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:20:30Z",
           |"is_read_only":true
           |},
           |{"id":"2fb15dd8-97b4-4c19-9886-6b5912ccb4d8",
           |"module":"strings",
           |"action":"update i18n string",
           |"status":"rejected",
           |"reason":null,
           |"created_at":"2019-01-03T15:50:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:50:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:50:30Z",
           |"is_read_only":true
           |},
           |{"id":"882baf82-a5c6-47f3-9a9a-14191d14b918",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:15:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:15:30Z",
           |"is_read_only":true
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria without filter [non-ceo]" in {
      val requesterLevel = 2
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks?order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"total":5,
           |"results":[
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"ecb907ae-ffaa-45da-abd2-3907fced637f",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T05:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T05:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"54403083-e0f1-4e80-bdd6-cfdaefd0646e",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"approved",
           |"reason":null,
           |"created_at":"2019-01-02T11:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:20:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:20:30Z",
           |"is_read_only":true
           |},
           |{"id":"2fb15dd8-97b4-4c19-9886-6b5912ccb4d8",
           |"module":"strings",
           |"action":"update i18n string",
           |"status":"rejected",
           |"reason":null,
           |"created_at":"2019-01-03T15:50:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:50:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:50:30Z",
           |"is_read_only":true
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria filter by module" in {
      val requesterLevel = 2
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks?&module=strings&order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"total":3,
           |"results":[
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"2fb15dd8-97b4-4c19-9886-6b5912ccb4d8",
           |"module":"strings",
           |"action":"update i18n string",
           |"status":"rejected",
           |"reason":null,
           |"created_at":"2019-01-03T15:50:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:50:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:50:30Z",
           |"is_read_only":true
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria filter by status" in {
      val requesterLevel = 2
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks?&status=rejected&order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"total":1,
           |"results":[
           |{"id":"2fb15dd8-97b4-4c19-9886-6b5912ccb4d8",
           |"module":"strings",
           |"action":"update i18n string",
           |"status":"rejected",
           |"reason":null,
           |"created_at":"2019-01-03T15:50:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:50:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:50:30Z",
           |"is_read_only":true
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria filter by date" in {
      val requesterLevel = 2
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks?date_from=2019-01-01&date_to=2019-01-02&order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"total":4,
           |"results":[
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T00:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"ecb907ae-ffaa-45da-abd2-3907fced637f",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T05:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T05:10:30Z",
           |"is_read_only":true
           |},
           |{"id":"54403083-e0f1-4e80-bdd6-cfdaefd0646e",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"approved",
           |"reason":null,
           |"created_at":"2019-01-02T11:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:20:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:20:30Z",
           |"is_read_only":true
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria filter by module,status,date" in {
      val requesterLevel = 2
      val requesterBusinessUnit = "BackOffice"

      val resp = route(app, FakeRequest(GET, s"/tasks?module=spreads&status=approved&date_from=2019-01-01&date_to=2019-01-02&order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderBuKey → requesterBusinessUnit,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"total":1,
           |"results":[
           |{"id":"54403083-e0f1-4e80-bdd6-cfdaefd0646e",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"approved",
           |"reason":null,
           |"created_at":"2019-01-02T11:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:20:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:20:30Z",
           |"is_read_only":true
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }
  }

  "MakerCheckerIntegrationTest API NEGATIVE READ" should {
    val apiKeyFromTrustedCaller = conf.get[String]("api-keys.backoffice-auth-api")

    "return error in getTasks by criteria when no businessUnit header" in {
      val requesterLevel = 2

      val resp = route(app, FakeRequest(GET, s"/tasks?module=spreads&status=approved&date_from=2019-01-01&date_to=2019-01-02&order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderApiKey → apiKeyFromTrustedCaller))).get

      val expected =
        s"""{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"Business unit not found in request headers"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe expected
    }

    "return error in getTasks by criteria when no api key is wrong" in {
      val requesterLevel = 2

      val resp = route(app, FakeRequest(GET, s"/tasks?module=spreads&status=approved&date_from=2019-01-01&date_to=2019-01-02&order_by=id")
        .withHeaders(jsonHeaders.add(
          requestRoleLevelKey → requesterLevel.toString,
          requestHeaderApiKey → "deadbeef"))).get

      val expected =
        s"""{"id":"$mockRequestId",
           |"code":"NotAuthorized",
           |"msg":"Source of request is not trusted"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe UNAUTHORIZED
      contentAsString(resp) mustBe expected
    }
  }

  "MakerCheckerIntegrationTest WITH AUTH" should {
    val apiKeyFromTrustedCaller = conf.get[String]("api-keys.backoffice-auth-api")

    "return success in detailed task in get by id " in {

      val resp = route(app, FakeRequest(GET, s"/api/tasks/06d18f41-1abf-4507-afab-5f8e1c7a1601")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"change":{"text":"hello","locale":"en-US","explanation":"text for hello world hahah","key":"hello","platform":"web"},
           |"original_value":null,
           |"is_read_only":false,
           |"stale":false
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "return success in getTasks by criteria without filter [CEO]" in {

      val resp = route(app, FakeRequest(GET, s"/api/tasks?order_by=id")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{"total":6,
           |"results":[
           |{"id":"06d18f41-1abf-4507-afab-5f8e1c7a1601",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:10:30Z",
           |"is_read_only":false
           |},
           |{"id":"e20773d8-38b6-4de0-bfdd-c3ca1b0ddbe8",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T00:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T00:10:30Z",
           |"is_read_only":false
           |},
           |{"id":"ecb907ae-ffaa-45da-abd2-3907fced637f",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-02T05:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-02T05:10:30Z",
           |"is_read_only":false
           |},
           |{"id":"54403083-e0f1-4e80-bdd6-cfdaefd0646e",
           |"module":"spreads",
           |"action":"create currency rate spreads",
           |"status":"approved",
           |"reason":null,
           |"created_at":"2019-01-02T11:10:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:20:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:20:30Z",
           |"is_read_only":false
           |},
           |{"id":"2fb15dd8-97b4-4c19-9886-6b5912ccb4d8",
           |"module":"strings",
           |"action":"update i18n string",
           |"status":"rejected",
           |"reason":null,
           |"created_at":"2019-01-03T15:50:30Z",
           |"created_by":"pegbuser",
           |"checked_at":"2019-01-05T15:50:30Z",
           |"checked_by":"george",
           |"updated_at":"2019-01-05T15:50:30Z",
           |"is_read_only":false
           |},
           |{"id":"882baf82-a5c6-47f3-9a9a-14191d14b918",
           |"module":"strings",
           |"action":"create i18n string",
           |"status":"pending",
           |"reason":null,
           |"created_at":"2019-01-01T00:15:30Z",
           |"created_by":"pegbuser",
           |"checked_at":null,
           |"checked_by":null,
           |"updated_at":"2019-01-01T00:15:30Z",
           |"is_read_only":false
           |}],
           |"limit":null,
           |"offset":null}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

  }
}