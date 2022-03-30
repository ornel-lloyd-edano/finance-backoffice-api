package tech.pegb.backoffice.api.i18n

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.i18n.abstraction.I18nStringManagement
import tech.pegb.backoffice.domain.i18n.dto.{I18nStringCriteria, I18nStringToCreate, I18nStringToUpdate}
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}
import tech.pegb.backoffice.domain.i18n.model.{I18nBulkInsertResult, I18nPair, I18nString}
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class I18nStringControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val i18nStringManagement = stub[I18nStringManagement]
  private val latestVersion = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[I18nStringManagement].to(i18nStringManagement),
      bind[LatestVersionService].to(latestVersion),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "I18nStringController" should {
    "create i18n string and respond with I18nString json in POST /strings " in {

      val jsonRequest =
        s"""{
           |  "key": "hello",
           |  "text": "hola",
           |  "platform": "web",
           |  "locale": "es",
           |  "type": "chat_message",
           |  "explanation": "hello in spanish"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val domainDto = I18nStringToCreate(
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        explanation = Some("hello in spanish"),
        `type` = "chat_message".some,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = I18nString(
        id = 1,
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_message".some,
        explanation = "hello in spanish".some,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (i18nStringManagement.createI18nString(_: I18nStringToCreate)(_: UUID)).when(domainDto, mockRequestId)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(POST, s"/strings", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":${expected.id},
           |"key":"hello",
           |"text":"hola",
           |"locale":"es",
           |"platform":"web",
           |"type":"chat_message",
           |"explanation":"hello in spanish",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "bulk upload should create" in {
      val jsonRequest =
        s"""
           |{
           |  "locale": "en-US",
           |  "strings": [
           |    {
           |      "key": "hello",
           |      "text": "hello",
           |      "locale": "en-US",
           |      "platform": "web",
           |      "type": "chat_message",
           |      "explanation": "text for hello"
           |    },
           |    {
           |      "key": "bye",
           |      "text": "Good Bye",
           |      "locale": "en-US",
           |      "platform": "web",
           |      "type": null,
           |      "explanation": "text for bye"
           |    }
           |  ]
           |}
         """.stripMargin

      val domainLocale = I18nLocale("en-US")
      val domainDto = Seq(
        I18nStringToCreate(
          key = I18nKey("hello"),
          text = I18nText("hello"),
          locale = I18nLocale("en-US"),
          platform = I18nPlatform("web"),
          explanation = Some("text for hello"),
          `type` = "chat_message".some,
          createdAt = mockRequestDate.toLocalDateTimeUTC),
        I18nStringToCreate(
          key = I18nKey("bye"),
          text = I18nText("Good Bye"),
          locale = I18nLocale("en-US"),
          platform = I18nPlatform("web"),
          explanation = Some("text for bye"),
          `type` = None,
          createdAt = mockRequestDate.toLocalDateTimeUTC))

      val expected = I18nBulkInsertResult(1, 1)

      (i18nStringManagement.bulkCreateI18nString _).when(domainLocale, domainDto)
        .returns(Future.successful(Right(expected)))

      val fakeRequest = FakeRequest(POST, s"/strings/bulk", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"inserted_count":1,
           |"updated_count":1
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson
    }

    "get I8n string by id" in {
      val id = 1
      val expected = I18nString(
        id = id,
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = None,
        explanation = None,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expectedJson =
        s"""
           |{"id":$id,
           |"key":"hello",
           |"text":"hola",
           |"locale":"es",
           |"platform":"web",
           |"type":null,
           |"explanation":null,
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (i18nStringManagement.getI18nStringById(_: Int)(_: UUID)).when(id, mockRequestId)
        .returns(Future.successful(Right(expected)))

      val resp = route(app, FakeRequest(GET, s"/strings/$id")
        .withHeaders(jsonHeaders)).get

      contentAsString(resp) mustBe expectedJson

    }

    "get I8n string by criteria" in {

      val i1 = I18nString(
        id = 1,
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = None,
        explanation = "hello in spanish".some,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val i2 = I18nString(
        id = 2,
        key = I18nKey("bye"),
        text = I18nText("adios"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = "chat_message".some,
        explanation = None,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{"id":1,
           |"key":"hello",
           |"text":"hola",
           |"locale":"es",
           |"platform":"web",
           |"type":null,
           |"explanation":"hello in spanish",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":2,
           |"key":"bye",
           |"text":"adios",
           |"locale":"es",
           |"platform":"web",
           |"type":"chat_message",
           |"explanation":null,
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val criteria = I18nStringCriteria(
        id = None,
        key = None,
        locale = Some(I18nLocale("es")),
        platform = None,
        explanation = None,
        partialMatchFields = Constants.i18nStringPartialMatchFields.filterNot(_ == "disabled"))

      val mockLatestVersion = LocalDateTime.now.toString
      val ordering = Seq(Ordering("platform", Ordering.ASCENDING), Ordering("key", Ordering.DESCENDING))

      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))
      (i18nStringManagement.countI18nStringByCriteria _).when(criteria)
        .returns(Future.successful(Right(2)))
      (i18nStringManagement.getI18nStringByCriteria _).when(criteria, ordering, None, None)
        .returns(Future.successful(Right(Seq(i1, i2))))

      val resp = route(app, FakeRequest(GET, s"/strings?locale=es&order_by=platform,-key")
        .withHeaders(jsonHeaders)).get

      contentAsString(resp) mustBe expectedJson

    }

    "get I18n pairs GET /i18n" in {
      val expectedJson =
        s"""
           |{
           |"hello":"hello",
           |"bye":"good bye",
           |"welcome":"Welcome user",
           |"cancel":"cancel"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val domainResponse = Future.successful(Seq(
        I18nPair(I18nKey("hello"), I18nText("hello")),
        I18nPair(I18nKey("bye"), I18nText("good bye")),
        I18nPair(I18nKey("welcome"), I18nText("Welcome user")),
        I18nPair(I18nKey("cancel"), I18nText("cancel"))).asRight[ServiceError])

      val criteria = I18nStringCriteria(
        locale = Some(I18nLocale("en")),
        platform = Some(I18nPlatform("web")))
      val mockLatestVersion = LocalDateTime.now.toString

      (i18nStringManagement.getI18nDictionary _).when(criteria)
        .returns(domainResponse)
      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(GET, s"/i18n?platform=web")
        .withHeaders(jsonHeaders.add((ACCEPT_LANGUAGE, "en")))).get

      contentAsString(resp) mustBe expectedJson
      headers(resp).get("x-version") mustBe mockLatestVersion.some
    }

    "get I18n pairs GET /i18n use default platform" in {
      val expectedJson =
        s"""
           |{
           |"hello":"hello",
           |"bye":"good bye",
           |"welcome":"Welcome user",
           |"cancel":"cancel"
           |}""".stripMargin.replace(System.lineSeparator(), "")

      val domainResponse = Future.successful(Seq(
        I18nPair(I18nKey("hello"), I18nText("hello")),
        I18nPair(I18nKey("bye"), I18nText("good bye")),
        I18nPair(I18nKey("welcome"), I18nText("Welcome user")),
        I18nPair(I18nKey("cancel"), I18nText("cancel"))).asRight[ServiceError])

      val criteria = I18nStringCriteria(
        locale = Some(I18nLocale("en")),
        platform = Some(I18nPlatform("web")))
      val mockLatestVersion = LocalDateTime.now.toString

      (i18nStringManagement.getI18nDictionary _).when(criteria)
        .returns(domainResponse)
      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(GET, s"/i18n")
        .withHeaders(jsonHeaders.add((ACCEPT_LANGUAGE, "en,en-us")))).get

      contentAsString(resp) mustBe expectedJson
      headers(resp).get("x-version") mustBe mockLatestVersion.some
    }

    "get I18n pairs HEAD /i18n use default platform" in {

      val criteria = I18nStringCriteria(
        locale = Some(I18nLocale("en")),
        platform = Some(I18nPlatform("web")))
      val mockLatestVersion = LocalDateTime.now.toString

      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(HEAD, s"/i18n")
        .withHeaders(jsonHeaders.add((ACCEPT_LANGUAGE, "en,en-us")))).get

      headers(resp).get("x-version") mustBe mockLatestVersion.some
    }

    "get I8n string by criteria invalid order by" in {
      val resp = route(app, FakeRequest(GET, s"/strings?locale=es&order_by=deadbeef")
        .withHeaders(jsonHeaders)).get

      status(resp) mustBe BAD_REQUEST
      (contentAsJson(resp) \ "msg").get.toString should include("invalid field for order_by found.")
    }

    "update i18n string" in {
      val id = 1
      val fakeUpdatedAt = LocalDateTime.now.toZonedDateTimeUTC
      val jsonRequest =
        s"""{
           |"key":"hello",
           |"text":"hola",
           |"locale":"es",
           |"platform":"web",
           |"explanation":null,
           |"updated_at":"$fakeUpdatedAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val i18nStringToUpdate = I18nStringToUpdate(
        key = I18nKey("hello").some,
        text = I18nText("hola").some,
        locale = I18nLocale("es").some,
        platform = I18nPlatform("web").some,
        explanation = None,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = fakeUpdatedAt.toLocalDateTimeUTC.some)

      val i1 = I18nString(
        id = 1,
        key = I18nKey("hello"),
        text = I18nText("hola"),
        locale = I18nLocale("es"),
        platform = I18nPlatform("web"),
        `type` = None,
        explanation = None,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (i18nStringManagement.updateI18nString(_: Int, _: I18nStringToUpdate)(_: UUID))
        .when(1, i18nStringToUpdate, *)
        .returns(Future.successful(Right(i1)))

      val fakeRequest = FakeRequest(PUT, s"/strings/$id",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":$id,
           |"key":"hello",
           |"text":"hola",
           |"locale":"es",
           |"platform":"web",
           |"type":null,
           |"explanation":null,
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
      status(resp) mustBe OK

    }

    "delete i18n string" in {
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (i18nStringManagement.deleteI18nString(_: Int, _: Option[LocalDateTime])(_: UUID))
        .when(1, fakeLastUpdateAt.toLocalDateTimeUTC.some, mockRequestId)
        .returns(Future.successful(Right(1)))

      val fakeRequest = FakeRequest(DELETE, s"/strings/1", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      contentAsString(resp) mustBe "1"
      status(resp) mustBe OK
    }

    "delete i18n string - precondition" in {
      val id = UUID.randomUUID()
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonRequest =
        s"""{"updated_at": "$fakeLastUpdateAt"}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (i18nStringManagement.deleteI18nString(_: Int, _: Option[LocalDateTime])(_: UUID))
        .when(1, fakeLastUpdateAt.toLocalDateTimeUTC.some, mockRequestId)
        .returns(Future.successful(Left(ServiceError.staleResourceAccessError(s"Delete failed. I18n String $id has been modified by another process.", mockRequestId.toOption))))

      val errorJson =
        s"""{"id":"$mockRequestId",
           |"code":"PreconditionFailed",
           |"msg":"Delete failed. I18n String $id has been modified by another process.",
           |"tracking_id":"$mockRequestId"}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val fakeRequest = FakeRequest(DELETE, s"/strings/1", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      status(resp) mustBe PRECONDITION_FAILED
      contentAsString(resp) mustBe errorJson
    }

  }
}
