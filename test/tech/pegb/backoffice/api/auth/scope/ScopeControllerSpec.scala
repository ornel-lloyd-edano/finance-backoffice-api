package tech.pegb.backoffice.api.auth.scope

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.ScopeManagement
import tech.pegb.backoffice.domain.auth.dto.{ScopeCriteria, ScopeToCreate, ScopeToUpdate}
import tech.pegb.backoffice.domain.auth.model.Scope
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}
import tech.pegb.backoffice.api.json.Implicits._

import scala.concurrent.Future

class ScopeControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val scopeManagement = stub[ScopeManagement]
  private val latestVersion = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[ScopeManagement].to(scopeManagement),
      bind[LatestVersionService].to(latestVersion),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "ScopeController" should {
    "create scope and respond with scope json in POST /scopes" in {
      val jsonRequest =
        s"""{
           |  "name": "accounts scope_scope",
           |  "parent_id": null,
           |  "description": "Some description for accounts scope"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ScopeToCreate(
        name = "accounts scope_scope",
        parentId = None,
        description = "Some description for accounts scope".some,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom)

      val expected = Scope(
        id = UUID.randomUUID(),
        parentId = None,
        name = "accounts",
        description = "Some description for accounts scope".some,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (scopeManagement.createScope _)
        .when(dto, false)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/scopes", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"${expected.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson

    }
    "reactivate scope vi POST /scopes" in {
      val jsonRequest =
        s"""{
           |  "name": "accounts scope_scope",
           |  "parent_id": null,
           |  "description": "Some description for accounts scope"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ScopeToCreate(
        name = "accounts scope_scope",
        parentId = None,
        description = "Some description for accounts scope".some,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom)

      val expected = Scope(
        id = UUID.randomUUID(),
        parentId = None,
        name = "accounts",
        description = "Some description for accounts scope".some,
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (scopeManagement.createScope _)
        .when(dto, true)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/scopes?reactivate=true", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"${expected.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"$mockRequestFrom",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson

    }

    "return scope json in GET /scopes/{id}" in {
      val id = UUID.randomUUID()

      val expected = Scope(
        id = id,
        parentId = None,
        name = "accounts",
        description = "Some description for accounts scope".some,
        createdBy = mockRequestFrom,
        createdAt = now,
        updatedBy = mockRequestFrom.some,
        updatedAt = now.some)

      (scopeManagement.getScopeById _).when(id)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/scopes/$id")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{"id":"${expected.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"$mockRequestFrom",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }
    "return all scopes and count as paginated result in GET /scopes/" in {

      val expected1 = Scope(
        id = UUID.randomUUID(),
        parentId = None,
        name = "accounts",
        description = "Some description for account scope".some,
        createdBy = mockRequestFrom,
        createdAt = now,
        updatedBy = mockRequestFrom.some,
        updatedAt = now.some)

      val expected2 = Scope(
        id = UUID.randomUUID(),
        parentId = expected1.id.some,
        name = "account_details",
        description = "Some description for account scope details".some,
        createdBy = mockRequestFrom,
        createdAt = now,
        updatedBy = mockRequestFrom.some,
        updatedAt = now.some)

      val mockLatestVersion = LocalDateTime.now.toString
      val criteria = ScopeCriteria()

      (scopeManagement.getScopeByCriteria _)
        .when(criteria, Nil, None, None)
        .returns(Future.successful(Seq(expected1, expected2).asRight[ServiceError]))
      (scopeManagement.countByCriteria _)
        .when(criteria)
        .returns(Future.successful(2.asRight[ServiceError]))
      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(GET, s"/scopes")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{"id":"${expected1.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for account scope",
           |"created_by":"$mockRequestFrom",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |},
           |{"id":"${expected2.id}",
           |"name":"account_details",
           |"parent_id":"${expected1.id}",
           |"description":"Some description for account scope details",
           |"created_by":"$mockRequestFrom",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${now.toZonedDateTimeUTC.toJsonStr}
           |}],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson

    }
    "update scopes matching id and respond with scope json in PUT /scopes/{id}" in {
      val id = UUID.randomUUID()
      val fakeUpdatedAt = LocalDateTime.now.toZonedDateTimeUTC

      val jsonRequest =
        s"""{
           |  "description": "new description",
           |  "updated_at": "$fakeUpdatedAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = ScopeToUpdate(
        description = "new description".some,
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = fakeUpdatedAt.toLocalDateTimeUTC.some)

      val expected = Scope(
        id = id,
        parentId = None,
        name = "accounts",
        description = "new description".some,
        createdBy = mockRequestFrom,
        createdAt = now,
        updatedBy = mockRequestFrom.some,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some)

      (scopeManagement.updateScopeById _)
        .when(id, dto)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(PUT, s"/scopes/$id",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{"id":"${expected.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"new description",
           |"created_by":"$mockRequestFrom",
           |"created_at":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"$mockRequestFrom",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${now.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
    "soft delete scope matching id in DELETE /scopes/{id}" in {
      val id = UUID.randomUUID()
      val fakeLastUpdateAt = LocalDateTime.now.toZonedDateTimeUTC

      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (scopeManagement.deleteScopeById _)
        .when(id, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, fakeLastUpdateAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(DELETE, s"/scopes/$id", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe id.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "delete scope matching id in DELETE /scopes/{id} even without json payload of update_at" in {
      val id = UUID.randomUUID()

      val jsonRequest = ""

      (scopeManagement.deleteScopeById _)
        .when(id, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, None)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(DELETE, s"/scopes/$id", jsonHeaders, jsonRequest)
      val resp = route(app, fakeRequest).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe id.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }
  }

}
