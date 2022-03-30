package tech.pegb.backoffice.api

import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.mvc.Headers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.notification.abstraction.NotificationManagementService
import tech.pegb.backoffice.domain.notification.dto._
import tech.pegb.backoffice.domain.notification.model._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.notification.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class NotificationMgmtControllerSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val ec = TestExecutionContext.genericOperations

  val notificationManager = stub[NotificationManagementService]
  val latestVersionService = stub[LatestVersionService]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[NotificationManagementService].to(notificationManager),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "NotificationManagementController" should {
    "get a notification template by id from GET /notification_templates/{id}" in {

      val mockId = UUID.randomUUID()
      val mockResult = NotificationTemplate(
        id = mockId,
        name = "email_template1",
        titleResource = "greetings email title",
        defaultTitle = "greetings email default title",
        contentResource = "greetings email content",
        defaultContent = "greetings email default content",
        description = "generic greeting message after successful activation".toOption,
        channels = Seq("email"),
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedAt = None,
        isActive = true)

      (notificationManager.getNotificationTemplatesByCriteria _)
        .when(mockId.asNotificationTemplateCriteria.toOption, Nil, None, None).returns(Right(Seq(mockResult)).toFuture)

      val response = route(app, FakeRequest(GET, s"/notification_templates/$mockId")).get

      val expected =
        s"""
           |{"id":"${mockId.toString}",
           |"name":"email_template1",
           |"title_resource":"greetings email title",
           |"default_title":"greetings email default title",
           |"content_resource":"greetings email content",
           |"default_content":"greetings email default content",
           |"description":"generic greeting message after successful activation",
           |"channels":["email"],
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"is_active":true
           |}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe OK
      contentAsString(response) mustEqual expected
    }

    "return NotFound if id was not found from GET /notification_templates/{id}" in {

      val mockId = UUID.randomUUID()

      (notificationManager.getNotificationTemplatesByCriteria _)
        .when(mockId.asNotificationTemplateCriteria.toOption, Nil, None, None).returns(Right(Nil).toFuture)

      val response = route(app, FakeRequest(GET, s"/notification_templates/$mockId")
        .withHeaders(Headers(CONTENT_TYPE → JSON, requestIdHeaderKey → mockRequestId.toString))).get

      val expected =
        s"""
           |{"id":"$mockRequestId",
           |"code":"NotFound",
           |"msg":"notification template with id: $mockId not found"}
           |
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe NOT_FOUND
      contentAsString(response) mustEqual expected
    }

    "get paginated notification templates from GET /notification_templates" in {

      val mockResults = Seq(
        NotificationTemplate(
          id = UUID.randomUUID(),
          name = "email_template1",
          titleResource = "greetings email title",
          defaultTitle = "greetings email default title",
          contentResource = "greetings email content",
          defaultContent = "greetings email default content",
          description = "generic greeting message after successful activation".toOption,
          channels = Seq("email"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedAt = None,
          isActive = true),
        NotificationTemplate(
          id = UUID.randomUUID(),
          name = "email_template2",
          titleResource = "greetings email title",
          defaultTitle = "greetings email default title",
          contentResource = "greetings email content",
          defaultContent = "greetings email default content",
          description = "generic greeting message after successful activation".toOption,
          channels = Seq("email"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedAt = None,
          isActive = true),
        NotificationTemplate(
          id = UUID.randomUUID(),
          name = "email_template3",
          titleResource = "greetings email title",
          defaultTitle = "greetings email default title",
          contentResource = "greetings email content",
          defaultContent = "greetings email default content",
          description = "generic greeting message after successful activation".toOption,
          channels = Seq("email"),
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedAt = None,
          isActive = true))

      val noCriteria = NotificationTemplateCriteria(partialMatchFields = Set("id", "name", "channel"))

      (notificationManager.countNotificationTemplatesByCriteria _)
        .when(noCriteria.toOption).returns(Right(mockResults.size).toFuture)

      (notificationManager.getNotificationTemplatesByCriteria _)
        .when(noCriteria.toOption, Nil, None, None).returns(Right(mockResults).toFuture)

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val response = route(app, FakeRequest(GET, s"/notification_templates")).get

      val expected =
        s"""
           |{"total":${mockResults.size},
           |"results":[
           |{"id":"${mockResults.head.id}",
           |"name":"${mockResults.head.name}",
           |"title_resource":"greetings email title",
           |"default_title":"greetings email default title",
           |"content_resource":"greetings email content",
           |"default_content":"greetings email default content",
           |"description":"generic greeting message after successful activation",
           |"channels":["email"],
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"is_active":true
           |},
           |{"id":"${mockResults(1).id}",
           |"name":"${mockResults(1).name}",
           |"title_resource":"greetings email title",
           |"default_title":"greetings email default title",
           |"content_resource":"greetings email content",
           |"default_content":"greetings email default content",
           |"description":"generic greeting message after successful activation",
           |"channels":["email"],
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"is_active":true
           |},
           |{"id":"${mockResults(2).id}",
           |"name":"${mockResults(2).name}",
           |"title_resource":"greetings email title",
           |"default_title":"greetings email default title",
           |"content_resource":"greetings email content",
           |"default_content":"greetings email default content",
           |"description":"generic greeting message after successful activation",
           |"channels":["email"],
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null,
           |"is_active":true
           |}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe OK
      contentAsString(response) mustEqual expected
      headers(response).contains(versionHeaderKey) mustBe true
      headers(response).get(versionHeaderKey) mustBe mockLatestVersion.toOption

    }

    "return BadRequest if invalid order_by query param in GET /notification_templates" in {

      val mockResults = Seq()

      val noCriteria = NotificationTemplateCriteria(partialMatchFields = Set("id", "name", "channel"))

      (notificationManager.countNotificationTemplatesByCriteria _)
        .when(noCriteria.toOption).returns(Right(mockResults.size).toFuture)

      (notificationManager.getNotificationTemplatesByCriteria _)
        .when(noCriteria.toOption, Nil, None, None).returns(Right(mockResults).toFuture)

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val response = route(app, FakeRequest(GET, s"/notification_templates?order_by=chicken_feet")
        .withHeaders(Headers(CONTENT_TYPE → JSON, requestIdHeaderKey → mockRequestId.toString))).get

      val expected =
        s"""
           |{"id":"${mockRequestId.toString}",
           |"code":"InvalidRequest",
           |"msg":"invalid value for order_by found. Valid values: [channel, created_at, id, name]"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe BAD_REQUEST
      contentAsString(response) mustEqual expected
    }

    "return BadRequest if invalid partial_match query param in GET /notification_templates" in {

      val mockResults = Seq()

      val noCriteria = NotificationTemplateCriteria(partialMatchFields = Set("id", "name", "channel"))

      (notificationManager.countNotificationTemplatesByCriteria _)
        .when(noCriteria.toOption).returns(Right(mockResults.size).toFuture)

      (notificationManager.getNotificationTemplatesByCriteria _)
        .when(noCriteria.toOption, Nil, None, None).returns(Right(mockResults).toFuture)

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val mockRequestId = UUID.randomUUID()

      val response = route(app, FakeRequest(GET, s"/notification_templates?partial_match=fried_duck")
        .withHeaders(Headers(CONTENT_TYPE → JSON, requestIdHeaderKey → mockRequestId.toString))).get

      val expected =
        s"""
           |{"id":"${mockRequestId.toString}",
           |"code":"InvalidRequest",
           |"msg":"invalid field for partial matching found. Valid fields: [channel, disabled, id, name]"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe BAD_REQUEST
      contentAsString(response) mustEqual expected
    }

    "get a notification by id from GET /notifications/{id}" in {

      val mockId = UUID.randomUUID()

      val mockResult = Notification(
        id = mockId,
        templateId = UUID.randomUUID(),
        operationId = "operation1",
        channel = "channel1",
        title = "title1",
        content = "content1",
        address = "address1",
        userId = UUID.randomUUID().toOption,
        status = "status1",
        sentAt = LocalDateTime.of(2019, 1, 1, 0, 0).toOption,
        errorMsg = None,
        retries = None,
        createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
        updatedAt = None)

      (notificationManager.getNotificationsByCriteria _)
        .when(mockId.asNotificationCriteria.toOption, Nil, None, None).returns(Right(Seq(mockResult)).toFuture)

      val response = route(app, FakeRequest(GET, s"/notifications/$mockId")).get

      val expected =
        s"""
           |{"id":"$mockId",
           |"template_id":"${mockResult.templateId}",
           |"operation_id":"operation1",
           |"channel":"channel1",
           |"title":"title1",
           |"content":"content1",
           |"address":"address1",
           |"user_id":"${mockResult.userId.get}",
           |"status":"status1",
           |"sent_at":"2019-01-01T00:00:00Z",
           |"error_msg":null,
           |"retries":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe OK
      contentAsString(response) mustEqual expected
    }

    "return NotFound if id was not found from GET /notifications/{id}" in {

      val mockId = UUID.randomUUID()

      (notificationManager.getNotificationsByCriteria _)
        .when(mockId.asNotificationCriteria.toOption, Nil, None, None).returns(Right(Nil).toFuture)

      val response = route(app, FakeRequest(GET, s"/notifications/$mockId")
        .withHeaders(Headers(CONTENT_TYPE → JSON, requestIdHeaderKey → mockRequestId.toString))).get

      val expected =
        s"""
           |{"id":"${mockRequestId}",
           |"code":"NotFound",
           |"msg":"notification with id: $mockId not found"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe NOT_FOUND
      contentAsString(response) mustEqual expected
    }

    "get paginated notifications from GET /notifications" in {

      val mockResults = Seq(
        Notification(
          id = UUID.randomUUID(),
          templateId = UUID.randomUUID(),
          operationId = "operation1",
          channel = "channel1",
          title = "title1",
          content = "content1",
          address = "address1",
          userId = UUID.randomUUID().toOption,
          status = "status1",
          sentAt = LocalDateTime.of(2019, 1, 1, 0, 0).toOption,
          errorMsg = None,
          retries = None,
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedAt = None),
        Notification(
          id = UUID.randomUUID(),
          templateId = UUID.randomUUID(),
          operationId = "operation2",
          channel = "channel2",
          title = "title2",
          content = "content2",
          address = "address2",
          userId = UUID.randomUUID().toOption,
          status = "status2",
          sentAt = LocalDateTime.of(2019, 1, 1, 0, 0).toOption,
          errorMsg = None,
          retries = None,
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedAt = None),
        Notification(
          id = UUID.randomUUID(),
          templateId = UUID.randomUUID(),
          operationId = "operation3",
          channel = "channel3",
          title = "title3",
          content = "content3",
          address = "address3",
          userId = UUID.randomUUID().toOption,
          status = "status3",
          sentAt = LocalDateTime.of(2019, 1, 1, 0, 0).toOption,
          errorMsg = None,
          retries = None,
          createdAt = LocalDateTime.of(2019, 1, 1, 0, 0),
          updatedAt = None))

      val noCriteria = NotificationCriteria()

      (notificationManager.countNotificationsByCriteria _)
        .when(noCriteria.toOption).returns(Right(mockResults.size).toFuture)

      (notificationManager.getNotificationsByCriteria _)
        .when(noCriteria.toOption, Nil, None, None).returns(Right(mockResults).toFuture)

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val response = route(app, FakeRequest(GET, s"/notifications")).get

      val expected =
        s"""
           |{"total":${mockResults.size},
           |"results":[
           |{"id":"${mockResults(0).id}",
           |"template_id":"${mockResults(0).templateId}",
           |"operation_id":"operation1",
           |"channel":"channel1",
           |"title":"title1",
           |"content":"content1",
           |"address":"address1",
           |"user_id":"${mockResults(0).userId.get}",
           |"status":"status1",
           |"sent_at":"2019-01-01T00:00:00Z",
           |"error_msg":null,
           |"retries":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null
           |},
           |{"id":"${mockResults(1).id}",
           |"template_id":"${mockResults(1).templateId}",
           |"operation_id":"operation2",
           |"channel":"channel2",
           |"title":"title2",
           |"content":"content2",
           |"address":"address2",
           |"user_id":"${mockResults(1).userId.get}",
           |"status":"status2",
           |"sent_at":"2019-01-01T00:00:00Z",
           |"error_msg":null,
           |"retries":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null
           |},
           |{"id":"${mockResults(2).id}",
           |"template_id":"${mockResults(2).templateId}",
           |"operation_id":"operation3",
           |"channel":"channel3",
           |"title":"title3",
           |"content":"content3",
           |"address":"address3",
           |"user_id":"${mockResults(2).userId.get}",
           |"status":"status3",
           |"sent_at":"2019-01-01T00:00:00Z",
           |"error_msg":null,
           |"retries":null,
           |"created_at":"2019-01-01T00:00:00Z",
           |"updated_at":null
           |}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe OK
      contentAsString(response) mustEqual expected
      headers(response).contains(versionHeaderKey) mustBe true
      headers(response).get(versionHeaderKey) mustBe mockLatestVersion.toOption

    }

    "return BadRequest if invalid order_by query param in GET /notifications" in {

      val mockResults = Seq()

      val noCriteria = NotificationCriteria()

      (notificationManager.countNotificationsByCriteria _)
        .when(noCriteria.toOption).returns(Right(mockResults.size).toFuture)

      (notificationManager.getNotificationsByCriteria _)
        .when(noCriteria.toOption, Nil, None, None).returns(Right(mockResults).toFuture)

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val response = route(app, FakeRequest(GET, s"/notifications?order_by=pork_leg")
        .withHeaders(Headers(CONTENT_TYPE → JSON, requestIdHeaderKey → mockRequestId.toString))).get

      val expected =
        s"""
           |{"id":"${mockRequestId.toString}",
           |"code":"InvalidRequest",
           |"msg":"invalid value for order_by found. Valid values: [address, channel, created_at, id, operation_id, retries, sent_at, status, template_id, title, user_id]"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe BAD_REQUEST
      contentAsString(response) mustEqual expected
    }

    "return BadRequest if invalid partial_match query param in GET /notifications" in {

      val mockResults = Seq()

      val noCriteria = NotificationCriteria()

      (notificationManager.countNotificationsByCriteria _)
        .when(noCriteria.toOption).returns(Right(mockResults.size).toFuture)

      (notificationManager.getNotificationsByCriteria _)
        .when(noCriteria.toOption, Nil, None, None).returns(Right(mockResults).toFuture)

      val mockLatestVersion = LocalDateTime.now.toString
      (latestVersionService.getLatestVersion _).when(noCriteria)
        .returns(Right(mockLatestVersion.toOption).toFuture)

      val mockRequestId = UUID.randomUUID()

      val response = route(app, FakeRequest(GET, s"/notifications?partial_match=beef_stew")
        .withHeaders(Headers(CONTENT_TYPE → JSON, requestIdHeaderKey → mockRequestId.toString))).get

      val expected =
        s"""
           |{"id":"${mockRequestId.toString}",
           |"code":"InvalidRequest",
           |"msg":"invalid field for partial matching found. Valid fields: [address, content, disabled, id, title, user_id]"}
         """.stripMargin.trim.replace(System.lineSeparator(), "")

      status(response) mustBe BAD_REQUEST
      contentAsString(response) mustEqual expected
    }

    "return created notification template in POST /notification_templates" in {

      val jsonRequest =
        s"""{
           |  "name": "template_1",
           |  "default_title": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "default_content": "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |  "channels": ["sms", "push"],
           |  "description": "description of template 1"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = NotificationTemplateToCreate(
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        name = "template_1",
        titleResource = "template_1_title",
        defaultTitle = "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
        contentResource = "template_1_content",
        defaultContent = "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
        channels = Seq("sms", "push"),
        description = "description of template 1".some)

      val expected = NotificationTemplate(
        id = UUID.randomUUID(),
        name = dto.name,
        titleResource = dto.titleResource,
        defaultTitle = dto.defaultTitle,
        contentResource = dto.contentResource,
        defaultContent = dto.defaultContent,
        description = dto.description,
        channels = dto.channels,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedAt = None,
        isActive = true)

      (notificationManager.createNotificationTemplate _)
        .when(dto)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(POST, s"/notification_templates", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"${expected.id}",
           |"name":"template_1",
           |"title_resource":"template_1_title",
           |"default_title":"ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |"content_resource":"template_1_content",
           |"default_content":"ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας",
           |"description":"description of template 1",
           |"channels":["sms","push"],
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":null,
           |"is_active":true
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson

    }

    "respond with 200 OK when deactivate notification template in PUT /notification_templates/:id/deactivate" in {
      val uuid = UUID.randomUUID()

      (notificationManager.deactivateNotificationTemplate _)
        .when(uuid, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future.successful(Right(())))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req = FakeRequest(PUT, s"/notification_templates/$uuid/deactivate", jsonHeaders, jsonRequest)
      val resp = route(app, req).get
      status(resp) mustBe OK
    }

    "respond with 200 OK when activate notification template in PUT /notification_templates/:id/activate" in {
      val uuid = UUID.randomUUID()

      (notificationManager.activateNotificationTemplate _)
        .when(uuid, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future.successful(Right(())))

      val jsonRequest =
        s"""{
           |"updated_at": null
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req = FakeRequest(PUT, s"/notification_templates/$uuid/activate", jsonHeaders, jsonRequest)
      val resp = route(app, req).get
      status(resp) mustBe OK
    }

    "respond with 200 OK when update notification template request is successful in PUT /notification_tempates" in {
      val mockId = UUID.randomUUID()

      val mockRequestDto1 = NotificationTemplateToUpdate(
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom,
        titleResource = "some title resource value".toOption,
        contentResource = "ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας".toOption)

      (notificationManager.updateNotificationTemplate _)
        .when(mockId, mockRequestDto1)
        .returns(Future.successful(Right(())))

      val jsonRequest1 =
        s"""{
           |"title_resource":"some title resource value",
           |"content_resource":"ÉÑñsdþ للترحيب 你好 हेलो こんにちは Привет γεια σας"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req1 = FakeRequest(PUT, s"/notification_templates/$mockId", jsonHeaders, jsonRequest1)
      val resp1 = route(app, req1).get
      status(resp1) mustBe OK

      val mockRequestDto2 = NotificationTemplateToUpdate(
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom,
        isActive = false.toOption)

      (notificationManager.updateNotificationTemplate _)
        .when(mockId, mockRequestDto2)
        .returns(Future.successful(Right(())))

      val jsonRequest2 =
        s"""{
           |"is_active":false
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req2 = FakeRequest(PUT, s"/notification_templates/$mockId", jsonHeaders, jsonRequest2)
      val resp2 = route(app, req2).get
      status(resp2) mustBe OK

      val mockRequestDto3 = NotificationTemplateToUpdate(
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom,
        name = "new name".toOption,
        description = "new description".toOption)

      (notificationManager.updateNotificationTemplate _)
        .when(mockId, mockRequestDto3)
        .returns(Future.successful(Right(())))

      val jsonRequest3 =
        s"""{
           |"name":"new name",
           |"description":"new description"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val req3 = FakeRequest(PUT, s"/notification_templates/$mockId", jsonHeaders, jsonRequest3)
      val resp3 = route(app, req3).get
      status(resp3) mustBe OK
    }

  }

}
