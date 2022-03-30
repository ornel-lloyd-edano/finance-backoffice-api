package tech.pegb.backoffice

import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import cats.implicits._
import org.scalatest.Matchers._
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentAsString, route, status, _ }
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.notification.dto.NotificationTemplateToRead
import tech.pegb.backoffice.util.Implicits._

class NotificationTemplateIntegrationTest extends PlayIntegrationTest {

  override def beforeAll(): Unit = {
    super.beforeAll()
    createSuperAdmin()
  }

  "NotificationTemplate API" should {
    "returns success in GET /notification_templates/:id" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates/b3f3ff13-0613-4d8c-83aa-463414ae3ead")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"id":"b3f3ff13-0613-4d8c-83aa-463414ae3ead",
           |"name":"template_1",
           |"title_resource":"template_1_title",
           |"default_title":"default title",
           |"content_resource":"template_1_content",
           |"default_content":"this is the default content",
           |"description":"",
           |"channels":["sms","email"],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in GET /notification_templates/:id even if channels were stored as json array in the database" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates/ddc893bc-804c-4992-9674-c77fc0462886")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"id":"ddc893bc-804c-4992-9674-c77fc0462886",
           |"name":"template_4",
           |"title_resource":"template_4_title",
           |"default_title":"default title 4",
           |"content_resource":"template_4_content",
           |"default_content":"this is the default content 4",
           |"description":"",
           |"channels":["\\"new_chann1\\"","\\"new_chann2\\""],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in GET /notification_templates/:id even if channels were stored as stringified json array in the database" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates/2d3df534-ae41-41cb-b2af-e045318894f7")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"id":"2d3df534-ae41-41cb-b2af-e045318894f7",
           |"name":"template_5",
           |"title_resource":"template_5_title",
           |"default_title":"default title 5",
           |"content_resource":"template_5_content",
           |"default_content":"this is the default content 5",
           |"description":"",
           |"channels":["\\"new_chann1\\"","\\"new_chann2\\""],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in GET /notification_templates without filter" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates?&order_by=id")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"total":5,
           |"results":[
           |{"id":"b3f3ff13-0613-4d8c-83aa-463414ae3ead",
           |"name":"template_1",
           |"title_resource":"template_1_title",
           |"default_title":"default title",
           |"content_resource":"template_1_content",
           |"default_content":"this is the default content",
           |"description":"",
           |"channels":["sms","email"],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |},
           |{"id":"32d755b2-b332-4850-bb72-406fa5eb95e5",
           |"name":"template_2",
           |"title_resource":"template_2_title",
           |"default_title":"default title 2",
           |"content_resource":"template_2_content",
           |"default_content":"this is the default content 2",
           |"description":"",
           |"channels":["sms","push"],
           |"created_at":"2019-02-21T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |},
           |{"id":"c43dd627-8521-4bcf-aebc-c4c148f2087c",
           |"name":"template_3",
           |"title_resource":"template_3_title",
           |"default_title":"default title 3",
           |"content_resource":"template_3_content",
           |"default_content":"this is the default content 3",
           |"description":"",
           |"channels":["push"],
           |"created_at":"2019-02-21T14:30:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false
           |},
           |{"id":"ddc893bc-804c-4992-9674-c77fc0462886",
           |"name":"template_4",
           |"title_resource":"template_4_title",
           |"default_title":"default title 4",
           |"content_resource":"template_4_content",
           |"default_content":"this is the default content 4",
           |"description":"",
           |"channels":["\\"new_chann1\\"","\\"new_chann2\\""],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false},
           |{"id":"2d3df534-ae41-41cb-b2af-e045318894f7",
           |"name":"template_5",
           |"title_resource":"template_5_title",
           |"default_title":"default title 5",
           |"content_resource":"template_5_content",
           |"default_content":"this is the default content 5",
           |"description":"",
           |"channels":["\\"new_chann1\\"","\\"new_chann2\\""],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false}
           |],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected

    }

    "returns success in GET /notification_templates with is_active filter" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates?is_active=true&order_by=-created_at")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"total":2,
           |"results":[
           |{"id":"32d755b2-b332-4850-bb72-406fa5eb95e5",
           |"name":"template_2",
           |"title_resource":"template_2_title",
           |"default_title":"default title 2",
           |"content_resource":"template_2_content",
           |"default_content":"this is the default content 2",
           |"description":"",
           |"channels":["sms","push"],
           |"created_at":"2019-02-21T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |},
           |{"id":"b3f3ff13-0613-4d8c-83aa-463414ae3ead",
           |"name":"template_1",
           |"title_resource":"template_1_title",
           |"default_title":"default title",
           |"content_resource":"template_1_content",
           |"default_content":"this is the default content",
           |"description":"",
           |"channels":["sms","email"],
           |"created_at":"2019-02-20T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in GET /notification_templates with channel filter" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates?channel=push&order_by=-created_at")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"total":2,
           |"results":[
           |{"id":"c43dd627-8521-4bcf-aebc-c4c148f2087c",
           |"name":"template_3",
           |"title_resource":"template_3_title",
           |"default_title":"default title 3",
           |"content_resource":"template_3_content",
           |"default_content":"this is the default content 3",
           |"description":"",
           |"channels":["push"],
           |"created_at":"2019-02-21T14:30:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false
           |},
           |{"id":"32d755b2-b332-4850-bb72-406fa5eb95e5",
           |"name":"template_2",
           |"title_resource":"template_2_title",
           |"default_title":"default title 2",
           |"content_resource":"template_2_content",
           |"default_content":"this is the default content 2",
           |"description":"",
           |"channels":["sms","push"],
           |"created_at":"2019-02-21T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected
    }

    "returns success in GET /notification_templates without filter with limit offset" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates?&order_by=id&limit=2&offset=1")
        .withHeaders(jsonHeaders)).get

      val expected =
        s"""{
           |"total":5,
           |"results":[
           |{"id":"32d755b2-b332-4850-bb72-406fa5eb95e5",
           |"name":"template_2",
           |"title_resource":"template_2_title",
           |"default_title":"default title 2",
           |"content_resource":"template_2_content",
           |"default_content":"this is the default content 2",
           |"description":"",
           |"channels":["sms","push"],
           |"created_at":"2019-02-21T00:00:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":true
           |},
           |{"id":"c43dd627-8521-4bcf-aebc-c4c148f2087c",
           |"name":"template_3",
           |"title_resource":"template_3_title",
           |"default_title":"default title 3",
           |"content_resource":"template_3_content",
           |"default_content":"this is the default content 3",
           |"description":"",
           |"channels":["push"],
           |"created_at":"2019-02-21T14:30:00Z",
           |"updated_at":"2019-02-20T00:00:00Z",
           |"is_active":false
           |}],
           |"limit":2,
           |"offset":1
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expected

    }

    var testId = UUID.randomUUID()
    var lastUpdatedAt = none[ZonedDateTime]
    "create a Notification Template" in {
      val jsonRequest =
        s"""{
           |  "name": "template_1",
           |  "default_title": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "default_content": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "channels": ["sms", "push"],
           |  "description": "description of template 1"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/notification_templates", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe CREATED
      val jsonResponse = contentAsString(resp).as(classOf[NotificationTemplateToRead]).toEither
      jsonResponse.map(_.name) mustBe Right("template_1")
      jsonResponse.map(_.defaultTitle) mustBe Right("ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.defaultContent) mustBe Right("ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.titleResource) mustBe Right("template_1_title")
      jsonResponse.map(_.contentResource) mustBe Right("template_1_content")
      jsonResponse.map(_.channels.toSet) mustBe Right(Set("sms", "push"))
      jsonResponse.map(_.description) mustBe Right("description of template 1".some)
      testId = jsonResponse.map(_.id).right.get

    }

    "retrieve Notification Template" in {
      val resp = route(app, FakeRequest(GET, s"/api/notification_templates/$testId")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe OK
      val jsonResponse = contentAsString(resp).as(classOf[NotificationTemplateToRead]).toEither
      jsonResponse.map(_.name) mustBe Right("template_1")
      jsonResponse.map(_.defaultTitle) mustBe Right("ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.defaultContent) mustBe Right("ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.titleResource) mustBe Right("template_1_title")
      jsonResponse.map(_.contentResource) mustBe Right("template_1_content")
      jsonResponse.map(_.channels.toSet) mustBe Right(Set("sms", "push"))
      jsonResponse.map(_.description) mustBe Right("description of template 1".some)
      jsonResponse.map(_.isActive) mustBe Right(true)
      testId = jsonResponse.map(_.id).right.get
      lastUpdatedAt = jsonResponse.map(_.updatedAt).right.get
    }

    "update a notification template" in {

      val jsonRequest =
        s"""{
           |  "title_resource": "new title resource",
           |  "content_resource": "new content resource",
           |  "default_title": "हेलो こんにちは Привет γεια σας",
           |  "default_content": "हेलो こんにちは Привет γεια σας",
           |  "channels":["email"],
           |  "updated_at":"${lastUpdatedAt.getOrElse(null)}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/notification_templates/$testId",
        jsonHeaders,
        jsonRequest)

      val resp = route(app, updateRequest).get
      status(resp) mustBe OK

      //check if updated
      val getResp = route(app, FakeRequest(GET, s"/api/notification_templates/$testId")
        .withHeaders(jsonHeaders)).get

      val jsonResponse = contentAsString(getResp).as(classOf[NotificationTemplateToRead]).toEither
      jsonResponse.map(_.name) mustBe Right("template_1")
      jsonResponse.map(_.defaultTitle) mustBe Right("हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.defaultContent) mustBe Right("हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.titleResource) mustBe Right("new title resource")
      jsonResponse.map(_.contentResource) mustBe Right("new content resource")
      jsonResponse.map(_.channels.toSet) mustBe Right(Set("email"))
      jsonResponse.map(_.description) mustBe Right("description of template 1".some)
      jsonResponse.map(_.updatedAt.map(_.toLocalDateTimeUTC)) mustBe Right(mockRequestDate.toLocalDateTimeUTC.some)
      testId = jsonResponse.map(_.id).right.get
      lastUpdatedAt = jsonResponse.map(_.updatedAt).right.get

    }

    "deactivate a notification template" in {
      val jsonRequest =
        s"""{
           |  "updated_at":"${lastUpdatedAt.getOrElse(null)}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/notification_templates/$testId/deactivate",
        jsonHeaders,
        jsonRequest)

      val resp = route(app, updateRequest).get
      status(resp) mustBe OK

      //check if updated
      val getResp = route(app, FakeRequest(GET, s"/api/notification_templates/$testId")
        .withHeaders(jsonHeaders)).get

      val jsonResponse = contentAsString(getResp).as(classOf[NotificationTemplateToRead]).toEither
      jsonResponse.map(_.name) mustBe Right("template_1")
      jsonResponse.map(_.defaultTitle) mustBe Right("हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.defaultContent) mustBe Right("हेलो こんにちは Привет γεια σας")
      jsonResponse.map(_.titleResource) mustBe Right("new title resource")
      jsonResponse.map(_.contentResource) mustBe Right("new content resource")
      jsonResponse.map(_.channels.toSet) mustBe Right(Set("email"))
      jsonResponse.map(_.description) mustBe Right("description of template 1".some)
      jsonResponse.map(_.updatedAt.map(_.toLocalDateTimeUTC)) mustBe Right(mockRequestDate.toLocalDateTimeUTC.some)
      jsonResponse.map(_.isActive) mustBe Right(false)
      testId = jsonResponse.map(_.id).right.get
      lastUpdatedAt = jsonResponse.map(_.updatedAt).right.get
    }

    "return badrequest when channel is not in descriptions table POST /notifaction_templates/{id}" in {
      val jsonRequest =
        s"""{
           |  "name": "template_1",
           |  "default_title": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "default_content": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "channels": ["sms", "deadbeef"],
           |  "description": "description of template 1"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(POST, s"/api/notification_templates", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe BAD_REQUEST
      (contentAsJson(resp) \ "msg").get.toString should include("provided element `deadbeef` is invalid for 'communication_channels'")

    }

    "return badrequest when updated_at is wrong (precondition_failed) in PUT /notification_templates/{id}" in {
      val jsonRequest =
        s"""{
           |  "title_resource": "new title resource",
           |  "content_resource": "new content resource",
           |  "default_title": "हेलो こんにちは Привет γεια σας",
           |  "default_content": "हेलो こんにちは Привет γεια σας",
           |  "channels":["email"],
           |  "updated_at":"${ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())}"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val updateRequest = FakeRequest(PUT, s"/api/notification_templates/$testId",
        jsonHeaders,
        jsonRequest)

      val resp = route(app, updateRequest).get
      status(resp) mustBe PRECONDITION_FAILED
      (contentAsJson(resp) \ "msg").get.toString should include("has been modified by another process.")

    }
  }
}
