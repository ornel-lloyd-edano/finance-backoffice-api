package tech.pegb.backoffice

import java.time.ZonedDateTime

import cats.implicits._
import org.scalatest.Matchers._
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers.{ POST, contentAsString, route, status }
import tech.pegb.backoffice.api.i18n.dto.I18nStringToRead
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Implicits._

class I18nStringIntegrationTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  "I18nString API POSITIVE READ" should {
    "returns success in getStrings by criteria without filter" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?&order_by=id")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
         |"total":8,
         |"results":[
         |{"id":1,
         |"key":"close",
         |"text":"close",
         |"locale":"en-US",
         |"platform":"web",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":2,
         |"key":"close",
         |"text":"close",
         |"locale":"en-US",
         |"platform":"ios",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":3,
         |"key":"close",
         |"text":"close",
         |"locale":"en-US",
         |"platform":"android",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":4,
         |"key":"how_are_you",
         |"text":"how are you?",
         |"locale":"en-US",
         |"platform":"web",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":5,
         |"key":"how_are_you",
         |"text":"how are you?",
         |"locale":"en-US",
         |"platform":"ios",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":6,
         |"key":"how_are_you",
         |"text":"how are you?",
         |"locale":"en-US",
         |"platform":"android",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":7,
         |"key":"close",
         |"text":"isara",
         |"locale":"fil-PH",
         |"platform":"web",
         |"type":null,
         |"explanation":null,
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |},
         |{"id":8,
         |"key":"how_are_you",
         |"text":"kumusta?",
         |"locale":"fil-PH",
         |"platform":"web",
         |"type":null,
         |"explanation":"tagalog how are you",
         |"created_at":"2019-02-20T00:00:00Z",
         |"updated_at":"2019-02-20T00:00:00Z"
         |}],
         |"limit":null,
         |"offset":null
         |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in getStrings by criteria - filter key" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?key=close&order_by=locale,-platform")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
                        |"total":4,
                        |"results":[
                        |{"id":1,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":2,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"ios",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":3,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"android",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":7,
                        |"key":"close",
                        |"text":"isara",
                        |"locale":"fil-PH",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |}],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in getStrings by criteria - filter key (partial match)" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?key=cl&order_by=locale,-platform")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
                        |"total":4,
                        |"results":[
                        |{"id":1,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":2,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"ios",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":3,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"android",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":7,
                        |"key":"close",
                        |"text":"isara",
                        |"locale":"fil-PH",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |}],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in getStrings by criteria with - filter locale" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?locale=fil-PH&order_by=key")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
                        |"total":2,
                        |"results":[
                        |{"id":7,
                        |"key":"close",
                        |"text":"isara",
                        |"locale":"fil-PH",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":8,
                        |"key":"how_are_you",
                        |"text":"kumusta?",
                        |"locale":"fil-PH",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":"tagalog how are you",
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |}],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in getStrings by criteria with - filter explanation" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?explanation=tagalog&order_by=key")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
                        |"total":1,
                        |"results":[
                        |{"id":8,
                        |"key":"how_are_you",
                        |"text":"kumusta?",
                        |"locale":"fil-PH",
                        |"platform":"web",
                        |"type":null,
                        |"explanation":"tagalog how are you",
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |}],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in getStrings by criteria with - filter platform and key" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?platform=android&key=close&order_by=created_at")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
                        |"total":1,
                        |"results":[
                        |{"id":3,
                        |"key":"close",
                        |"text":"close",
                        |"locale":"en-US",
                        |"platform":"android",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |}],
                        |"limit":null,
                        |"offset":null
                        |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")
      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in getStrings by criteria with limit and offset" in {

      val resp = route(app, FakeRequest(GET, s"/api/strings?&limit=2&offset=4&order_by=id")
        .withHeaders(jsonHeaders)).get

      val expected = s"""{
                        |"total":8,
                        |"results":[
                        |{"id":5,
                        |"key":"how_are_you",
                        |"text":"how are you?",
                        |"locale":"en-US",
                        |"platform":"ios",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |},
                        |{"id":6,
                        |"key":"how_are_you",
                        |"text":"how are you?",
                        |"locale":"en-US",
                        |"platform":"android",
                        |"type":null,
                        |"explanation":null,
                        |"created_at":"2019-02-20T00:00:00Z",
                        |"updated_at":"2019-02-20T00:00:00Z"
                        |}],
                        |"limit":2,
                        |"offset":4
                        |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success and dictionary in GET /i18n with platform" in {

      val resp = route(app, FakeRequest(GET, s"/api/i18n?platform=ios")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val jsonResp = contentAsJson(resp)
      (jsonResp \ "close").get.as[String] mustBe "close"
      (jsonResp \ "how_are_you").get.as[String] mustBe "how are you?"

      headers(resp).get("x-version") mustBe "2019-02-20T00:00".some
    }

    "returns success and dictionary in GET /i18n use default platform" in {

      val resp = route(app, FakeRequest(GET, s"/api/i18n")
        .withHeaders(jsonHeaders.add((ACCEPT_LANGUAGE, "fil-PH")))).get

      status(resp) mustBe OK
      val jsonResp = contentAsJson(resp)
      (jsonResp \ "close").get.as[String] mustBe "isara"
      (jsonResp \ "how_are_you").get.as[String] mustBe "kumusta?"
      headers(resp).get("x-version") mustBe "2019-02-20T00:00".some
    }

    "returns latestversion in HEAD /i18n " in {

      val resp = route(app, FakeRequest(HEAD, s"/api/i18n")
        .withHeaders(jsonHeaders.add((ACCEPT_LANGUAGE, "fil-PH")))).get

      headers(resp).get("x-version") mustBe "2019-02-20T00:00".some
    }


  }

  "I18nString API POSITIVE TESTS" should {
    var testId: Int = 0
    "create i18n_string row successfully using POST /api/strings" in {

      val jsonRequest =
        s"""{
           |  "key": "hello",
           |  "text": "hola",
           |  "platform": "web",
           |  "locale": "es",
           |  "type": "chat_message"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/strings", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsString(resp).as(classOf[I18nStringToRead]).toEither
      jsonResponse.map(_.key) mustBe Right("hello")
      jsonResponse.map(_.text) mustBe Right("hola")
      jsonResponse.map(_.platform) mustBe Right("web")
      jsonResponse.map(_.locale) mustBe Right("es")
      jsonResponse.map(_.`type`) mustBe Right("chat_message".some)
      jsonResponse.map(_.explanation) mustBe Right(None)
      testId = jsonResponse.map(_.id).right.get
    }

    "get i18n_string row using GET /api/strings/:id" in {
      val fakeRequest = FakeRequest(GET, s"/api/strings/$testId").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[I18nStringToRead]).toEither
      jsonResponse.map(_.key) mustBe Right("hello")
      jsonResponse.map(_.text) mustBe Right("hola")
      jsonResponse.map(_.platform) mustBe Right("web")
      jsonResponse.map(_.locale) mustBe Right("es")
      jsonResponse.map(_.explanation) mustBe Right(None)

    }

    "create, retrieve, update, delete" in {

      //CREATE
      val jsonRequest =
        s"""{
           |  "key": "hello",
           |  "text": "hola",
           |  "platform": "ios",
           |  "locale": "es",
           |  "type": "chat_messages",
           |  "explanation": "spanish hello"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/strings", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsString(resp).as(classOf[I18nStringToRead]).toEither
      jsonResponse.map(_.key) mustBe Right("hello")
      jsonResponse.map(_.text) mustBe Right("hola")
      jsonResponse.map(_.platform) mustBe Right("ios")
      jsonResponse.map(_.locale) mustBe Right("es")
      jsonResponse.map(_.`type`) mustBe Right("chat_messages".some)
      jsonResponse.map(_.explanation) mustBe Right("spanish hello".some)
      val createdId = jsonResponse.map(_.id).right.get

      //GET
      val getRequest = FakeRequest(GET, s"/api/strings/$createdId").withHeaders(jsonHeaders)
      val getResp = route(app, getRequest).get

      status(getResp) mustBe OK
      jsonResponse.map(_.key) mustBe Right("hello")
      jsonResponse.map(_.text) mustBe Right("hola")
      jsonResponse.map(_.platform) mustBe Right("ios")
      jsonResponse.map(_.locale) mustBe Right("es")
      jsonResponse.map(_.`type`) mustBe Right("chat_messages".some)
      jsonResponse.map(_.explanation) mustBe Right("spanish hello".some)
      val lastUpdated = jsonResponse.map(_.updatedAt).right.get

      //UPDATE
      val updateJson =
        s"""{
           |"key":"apple",
           |"text":"ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας!!",
           |"locale":"de",
           |"platform":"web",
           |"type":"chat_message",
           |"explanation":"apple in german",
           |"updated_at":"${lastUpdated.get}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/strings/$createdId",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      val updateJsonResult =
        s"""
           |{"id":${createdId},
           |"key":"apple",
           |"text":"ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας!!",
           |"locale":"de",
           |"platform":"web",
           |"type":"chat_message",
           |"explanation":"apple in german",
           |"created_at":"${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}",
           |"updated_at":"${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(updateResp) mustBe OK
      contentAsString(updateResp) mustBe updateJsonResult

      val getResponse2 = contentAsString(updateResp).as(classOf[I18nStringToRead]).toEither
      val getRespUpdatedAt2 = getResponse2.map(_.updatedAt).right.get

      //DELETE
      val deleteRequestJson =
        s"""{
           |"updated_at": "${getRespUpdatedAt2.get}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val deleteRequest = FakeRequest(DELETE, s"/api/strings/$createdId", jsonHeaders, deleteRequestJson.toString)
      val deleteResp = route(app, deleteRequest).get

      status(deleteResp) mustBe OK
      contentAsString(deleteResp) mustBe s"$createdId"
    }

  }

  "I18nString API NEGATIVE tests" should {

    "return error in getStringById " in {
      val fakeId = 0
      val fakeRequest = FakeRequest(GET, s"/api/strings/$fakeId").withHeaders(jsonHeaders)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe NOT_FOUND
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "msg").get.toString() should include("I18n String id [0] not found")
      (jsonResponse \ "code").get.toString() should include("NotFound")
    }

    "return error in create i18n_string row when invalid platform" in {

      val jsonRequest =
        s"""{
           |  "key": "hello",
           |  "text": "hola",
           |  "platform": "DEADBEEF",
           |  "locale": "es"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/strings", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      contentAsJson(resp).as[JsObject].keys.contains("tracking_id") mustBe true
      val respWithoutTrackingId = contentAsJson(resp).as[JsObject].-("tracking_id").toString()

      val expected =
        s"""
           |{"id":"$mockRequestId",
           |"code":"InvalidRequest",
           |"msg":"provided element `DEADBEEF` is invalid for 'platform'"
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe BAD_REQUEST
      respWithoutTrackingId mustBe expected
    }

    "return error in create i18n_string row when key, platform and locale already exists" in {

      val jsonRequest =
        s"""{
           |  "key": "hello",
           |  "text": "HOLLLLLLLLLA",
           |  "platform": "web",
           |  "locale": "es"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/strings", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      val jsonResponse = contentAsJson(resp)
      (jsonResponse \ "msg").get.toString() should include("Could not create i18n string")
    }

    "get I8n string by criteria invalid order by" in {
      val resp = route(app, FakeRequest(GET, s"/api/strings?locale=es&order_by=deadbeef")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe BAD_REQUEST
      (contentAsJson(resp) \ "msg").get.toString should include("invalid field for order_by found.")

    }

    "update fail (precondition test)" in {
      val fakeUpdatedAt = ZonedDateTime.now()
      //UPDATE
      val updateJson =
        s"""{
           |"key":"apple",
           |"text":"apfel!!",
           |"locale":"de",
           |"platform":"web",
           |"updated_at":"${fakeUpdatedAt}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/strings/1",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      status(updateResp) mustBe PRECONDITION_FAILED
    }

    "update fail (notfound)" in {
      val fakeUpdatedAt = ZonedDateTime.now()
      //UPDATE
      val updateJson =
        s"""{
           |"key":"apple",
           |"text":"apfel!!",
           |"locale":"de",
           |"platform":"web",
           |"updated_at":"${fakeUpdatedAt}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/strings/0",
        jsonHeaders,
        updateJson)

      val updateResp = route(app, updateRequest).get

      status(updateResp) mustBe NOT_FOUND
    }

    "delete fail (precondition test)" in {
      val deleteRequestJson =
        s"""{
           |"updated_at": "${ZonedDateTime.now}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val deleteRequest = FakeRequest(DELETE, s"/api/strings/1", jsonHeaders, deleteRequestJson.toString)
      val deleteResp = route(app, deleteRequest).get
      status(deleteResp) mustBe PRECONDITION_FAILED
    }

    "delete fail (notfound)" in {
      //DELETE
      val deleteRequestJson =
        s"""{
           |"updated_at": "${ZonedDateTime.now}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/strings/0",
        jsonHeaders,
        deleteRequestJson)

      val updateResp = route(app, updateRequest).get

      status(updateResp) mustBe NOT_FOUND
    }
  }

  "I18nString bulk upload" should {
    "insert all new only" in {
      val jsonRequest =
        s"""
           |{
           |  "locale": "fr",
           |  "strings": [
           |    {
           |      "key": "hello",
           |      "text": "salut",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": "chat_message",
           |      "explanation": "text for french"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "au revoir",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": "chat_message",
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "good",
           |      "text": "bien",
           |      "locale": "fr",
           |      "platform": "android",
           |      "type": "chat_message",
           |      "explanation": "text for good"
           |    },
           |    {
           |      "key": "friend",
           |      "text": "amie",
           |      "locale": "fr",
           |      "platform": "ios",
           |      "type": "chat_messages",
           |      "explanation": "text for friend"
           |    }
           |  ]
           |}
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/strings/bulk", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"inserted_count":4,
           |"updated_count":0
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "update all old only" in {
      val jsonRequest =
        s"""
           |{
           |  "locale": "fr",
           |  "strings": [
           |    {
           |      "key": "hello",
           |      "text": "salut",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for french"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "au revoir",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "good",
           |      "text": "Bien",
           |      "locale": "fr",
           |      "platform": "android",
           |      "type": "chat_message",
           |      "explanation": "text for good"
           |    },
           |    {
           |      "key": "friend",
           |      "text": "Amie",
           |      "locale": "fr",
           |      "platform": "ios",
           |      "type": "chat_messages",
           |      "explanation": "text for friend"
           |    }
           |  ]
           |}
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/strings/bulk", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"inserted_count":0,
           |"updated_count":4
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }
    "insert new and update old" in {
      val jsonRequest =
        s"""
           |{
           |  "locale": "fr",
           |  "strings": [
           |    {
           |      "key": "hello",
           |      "text": "Salut",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for french"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir android",
           |      "locale": "fr",
           |      "platform": "android",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir ios",
           |      "locale": "fr",
           |      "platform": "ios",
           |      "type": "chat_messages",
           |      "explanation": "text for bye"
           |    }
           |  ]
           |}
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/strings/bulk", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"inserted_count":2,
           |"updated_count":2
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "return error when a dto locale doesnt match main locale" in {
      val jsonRequest =
        s"""
           |{
           |  "locale": "fr",
           |  "strings": [
           |    {
           |      "key": "hello",
           |      "text": "Salut",
           |      "locale": "en",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for french"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir android",
           |      "locale": "fr",
           |      "platform": "android",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir ios",
           |      "locale": "fr",
           |      "platform": "ios",
           |      "type": "chat_messages",
           |      "explanation": "text for bye"
           |    }
           |  ]
           |}
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/strings/bulk", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("Items with different locale from fr are found")
    }

    "return error when there are duplicates" in {
      val jsonRequest =
        s"""
           |{
           |  "locale": "fr",
           |  "strings": [
           |    {
           |      "key": "hello",
           |      "text": "Salut",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for french"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir",
           |      "locale": "fr",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for bye"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Au revoir ios",
           |      "locale": "fr",
           |      "platform": "ios",
           |      "type": "chat_messages",
           |      "explanation": "text for bye"
           |    }
           |  ]
           |}
         """.stripMargin

      val fakeRequest = FakeRequest(POST, s"/api/strings/bulk", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) should include("Duplicates found in items to create")
    }
  }

}
