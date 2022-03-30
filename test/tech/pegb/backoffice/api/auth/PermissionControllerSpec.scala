package tech.pegb.backoffice.api.auth

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.auth.controllers.impl.PermissionController
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.domain.auth.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.PermissionManagement
import tech.pegb.backoffice.domain.auth.dto.{PermissionCriteria, PermissionToCreate, PermissionToUpdate}
import tech.pegb.backoffice.domain.auth.model.{Permission, Scope}
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class PermissionControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val permissionManagement = stub[PermissionManagement]
  private val latestVersion = stub[LatestVersionService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[PermissionManagement].to(permissionManagement),
      bind[LatestVersionService].to(latestVersion),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "PermissionController" should {

    "create permission and respond with permission json in POST /permissions" in {
      val jsonRequest =
        s"""{
           |"scope_id":"ed4be9e3-bf3c-4268-aef9-b8c843babd4c",
           |"permission_key":{"role_id":"5c095b24-c695-45a8-9bac-abef6366acda","bu_id":"931f11af-9aca-417d-b910-0557f1f3ac00"}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = PermissionToCreate(
        permissionKey = BusinessUnitAndRolePermissionKey(
          buId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00"),
          roleId = UUID.fromString("5c095b24-c695-45a8-9bac-abef6366acda")),
        revoke = None,
        scopeId = UUID.fromString("ed4be9e3-bf3c-4268-aef9-b8c843babd4c"),
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = Permission(
        id = UUID.randomUUID(),
        permissionKey = BusinessUnitAndRolePermissionKey(
          buId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00"),
          roleId = UUID.fromString("5c095b24-c695-45a8-9bac-abef6366acda")),
        scope = Scope(
          id = UUID.randomUUID(),
          parentId = None,
          name = "accounts",
          description = "Some description for accounts scope".some,
          createdBy = mockRequestFrom,
          createdAt = mockRequestDate.toLocalDateTimeUTC,
          updatedBy = mockRequestFrom.some,
          updatedAt = mockRequestDate.toLocalDateTimeUTC.some),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        updatedBy = mockRequestFrom.some)

      (permissionManagement.createPermission _)
        .when(dto, false)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/permissions", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"id":"${expected.id}",
           |"scope":
           |{"id":"${expected.scope.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson

    }
    "reactivate respond with permission json in POST /permissions" in {
      val jsonRequest =
        s"""{
           |"scope_id":"ed4be9e3-bf3c-4268-aef9-b8c843babd4c",
           |"permission_key":{"user_id":"931f11af-9aca-417d-b910-0557f1f3ac00"}
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = PermissionToCreate(
        permissionKey = UserPermissionKey(
          userId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00")),
        revoke = None,
        scopeId = UUID.fromString("ed4be9e3-bf3c-4268-aef9-b8c843babd4c"),
        createdBy = mockRequestFrom,
        createdAt = mockRequestDate.toLocalDateTimeUTC)

      val expected = Permission(
        id = UUID.randomUUID(),
        permissionKey = UserPermissionKey(
          userId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00")),
        scope = Scope(
          id = UUID.randomUUID(),
          parentId = None,
          name = "accounts",
          description = "Some description for accounts scope".some,
          createdBy = mockRequestFrom,
          createdAt = mockRequestDate.toLocalDateTimeUTC,
          updatedBy = mockRequestFrom.some,
          updatedAt = mockRequestDate.toLocalDateTimeUTC.some),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        updatedBy = mockRequestFrom.some)

      (permissionManagement.createPermission _)
        .when(dto, true)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(POST, s"/permissions?reactivate=true", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"id":"${expected.id}",
           |"scope":
           |{"id":"${expected.scope.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe CREATED
      contentAsString(resp) mustBe expectedJson

    }
    "return permission json in GET /permissions/{id}" in {
      val id = UUID.randomUUID()

      val expected = Permission(
        id = UUID.randomUUID(),
        permissionKey = UserPermissionKey(
          userId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00")),
        scope = Scope(
          id = UUID.randomUUID(),
          parentId = None,
          name = "accounts",
          description = "Some description for accounts scope".some,
          createdBy = mockRequestFrom,
          createdAt = mockRequestDate.toLocalDateTimeUTC,
          updatedBy = mockRequestFrom.some,
          updatedAt = mockRequestDate.toLocalDateTimeUTC.some),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        updatedBy = mockRequestFrom.some)

      (permissionManagement.getPermissionById _).when(id)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val resp = route(app, FakeRequest(GET, s"/permissions/$id")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""
           |{
           |"id":"${expected.id}",
           |"scope":
           |{"id":"${expected.scope.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson
    }
    "return all permissions and count as paginated result in GET /permissions/" in {
      val expected1 = Permission(
        id = UUID.randomUUID(),
        permissionKey = UserPermissionKey(
          userId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00")),
        scope = Scope(
          id = UUID.randomUUID(),
          parentId = None,
          name = "accounts",
          description = "Some description for accounts scope".some,
          createdBy = mockRequestFrom,
          createdAt = mockRequestDate.toLocalDateTimeUTC,
          updatedBy = mockRequestFrom.some,
          updatedAt = mockRequestDate.toLocalDateTimeUTC.some),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        updatedBy = mockRequestFrom.some)

      val expected2 = Permission(
        id = UUID.randomUUID(),
        permissionKey = BusinessUnitAndRolePermissionKey(
          buId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00"),
          roleId = UUID.fromString("5c095b24-c695-45a8-9bac-abef6366acda")),
        scope = Scope(
          id = UUID.randomUUID(),
          parentId = None,
          name = "accounts",
          description = "Some description for customer scope".some,
          createdBy = mockRequestFrom,
          createdAt = mockRequestDate.toLocalDateTimeUTC,
          updatedBy = mockRequestFrom.some,
          updatedAt = mockRequestDate.toLocalDateTimeUTC.some),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        updatedBy = mockRequestFrom.some)

      val mockLatestVersion = LocalDateTime.now.toString
      val criteria = PermissionCriteria(partialMatchFields = PermissionController.permissionPartialMatchFields.filterNot(_ === "disabled"))

      (permissionManagement.getPermissionByCriteria _)
        .when(criteria, Nil, None, None)
        .returns(Future.successful(Seq(expected1, expected2).asRight[ServiceError]))
      (permissionManagement.countByCriteria _)
        .when(criteria)
        .returns(Future.successful(2.asRight[ServiceError]))
      (latestVersion.getLatestVersion _).when(criteria).returns(Future.successful(Right(mockLatestVersion.some)))

      val resp = route(app, FakeRequest(GET, s"/permissions")
        .withHeaders(jsonHeaders)).get

      val expectedJson =
        s"""{
           |"total":2,
           |"results":[
           |{
           |"id":"${expected1.id}",
           |"scope":
           |{"id":"${expected1.scope.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |{
           |"id":"${expected2.id}",
           |"scope":
           |{"id":"${expected2.scope.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for customer scope",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}
           |],
           |"limit":null,
           |"offset":null
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      contentAsString(resp) mustBe expectedJson
    }
    "update permissions matching id and respond with permission json in PUT /permissions/{id}" in {
      val id = UUID.randomUUID()
      val fakeUpdatedAt = LocalDateTime.now.toZonedDateTimeUTC

      val jsonRequest =
        s"""{
           |  "permission_key":{"role_id":"5c095b24-c695-45a8-9bac-abef6366acda","bu_id":"931f11af-9aca-417d-b910-0557f1f3ac00"},
           |  "updated_at": "$fakeUpdatedAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      val dto = PermissionToUpdate(
        permissionKey = BusinessUnitAndRolePermissionKey(
          buId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00"),
          roleId = UUID.fromString("5c095b24-c695-45a8-9bac-abef6366acda")).some,
        updatedBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = fakeUpdatedAt.toLocalDateTimeUTC.some)

      val expected = Permission(
        id = id,
        permissionKey = BusinessUnitAndRolePermissionKey(
          buId = UUID.fromString("931f11af-9aca-417d-b910-0557f1f3ac00"),
          roleId = UUID.fromString("5c095b24-c695-45a8-9bac-abef6366acda")),
        scope = Scope(
          id = UUID.randomUUID(),
          parentId = None,
          name = "accounts",
          description = "Some description for accounts scope".some,
          createdBy = mockRequestFrom,
          createdAt = mockRequestDate.toLocalDateTimeUTC,
          updatedBy = mockRequestFrom.some,
          updatedAt = mockRequestDate.toLocalDateTimeUTC.some),
        createdAt = mockRequestDate.toLocalDateTimeUTC,
        createdBy = mockRequestFrom,
        updatedAt = mockRequestDate.toLocalDateTimeUTC.some,
        updatedBy = mockRequestFrom.some)

      (permissionManagement.updatePermissionById _)
        .when(id, dto)
        .returns(Future.successful(expected.asRight[ServiceError]))

      val fakeRequest = FakeRequest(PUT, s"/permissions/$id",
        jsonHeaders,
        jsonRequest.toString)

      val resp = route(app, fakeRequest).get

      val expectedJson =
        s"""
           |{
           |"id":"$id",
           |"scope":
           |{"id":"${expected.scope.id}",
           |"name":"accounts",
           |"parent_id":null,
           |"description":"Some description for accounts scope",
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |},
           |"created_by":"pegbuser",
           |"created_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_by":"pegbuser",
           |"updated_at":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"created_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${mockRequestDate.toLocalDateTimeUTC.toZonedDateTimeUTC.toJsonStr}
           |}""".stripMargin.trim.replaceAll(System.lineSeparator(), "")

      status(resp) mustBe OK
      contentAsString(resp) mustBe expectedJson

    }
    "soft delete permission matching id in DELETE /permissions/{id}" in {
      val id = UUID.randomUUID()
      val fakeLastUpdateAt = LocalDateTime.now.toZonedDateTimeUTC

      val jsonRequest =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}
         """.stripMargin.trim.replaceAll(System.lineSeparator(), "")

      (permissionManagement.deletePermissionById _)
        .when(id, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, fakeLastUpdateAt.toLocalDateTimeUTC.some)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(DELETE, s"/permissions/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe id.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "delete permission even without json payload for backwards compatibility" in {
      val id = UUID.randomUUID()

      val jsonRequest = ""

      (permissionManagement.deletePermissionById _)
        .when(id, mockRequestDate.toLocalDateTimeUTC, mockRequestFrom, None)
        .returns(Future.successful(Right(())))

      val fakeRequest = FakeRequest(DELETE, s"/permissions/$id", jsonHeaders, jsonRequest.toString)
      val resp = route(app, fakeRequest).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe id.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }
  }

}
