package tech.pegb.backoffice.api.auth

import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.auth.dto.{RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.domain.auth.abstraction.RoleService
import tech.pegb.backoffice.domain.auth.dto.RoleCriteria
import tech.pegb.backoffice.domain.auth.model.Role
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.auth.role.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.{ExecutionContext, Future}

class RoleControllerSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val ec: ExecutionContext = TestExecutionContext.genericOperations

  private val roleService = stub[RoleService]
  private val latestVersionService: LatestVersionService = stub[LatestVersionService]

  private val mockClock = Clock.fixed(Instant.ofEpochMilli(1571646258000L), ZoneId.of("UTC"))

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[RoleService].to(roleService),
      bind[LatestVersionService].to(latestVersionService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "RoleController" should {
    val mockId = UUID.randomUUID()

    "create a role in POST /roles" in {
      val jsonBody = """{"name": "test_role", "level": 1}""".stripMargin
      val roleToCreate = RoleToCreate(name = "test_role", level = 1).asDomain(mockRequestDate, mockRequestFrom)
      val role = Role(
        id = mockId,
        name = "test_role",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)

      (roleService.createActiveRole _).when(roleToCreate, false).returns(Future.successful(Right(role)))
      val resp = route(app, FakeRequest(POST, s"/roles", jsonHeaders, jsonBody)).get
      val expectedJson =
        s"""
           |{"id":"${role.id}",
           |"name":"test_role",
           |"level":1,
           |"created_by":"${role.createdBy}",
           |"updated_by":"${role.updatedBy.get}",
           |"created_at":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr},
           |"created_time":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe CREATED

      contentAsString(resp) mustBe expectedJson
    }

    "recreate a role that was previously deleted in POST /roles?reactivate=true" in {
      val jsonBody = """{"name": "test_role", "level": 1}""".stripMargin
      val roleToCreate = RoleToCreate(name = "test_role", level = 1).asDomain(mockRequestDate, mockRequestFrom)
      val role = Role(
        id = mockId,
        name = "test_role",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)

      (roleService.createActiveRole _).when(roleToCreate, true).returns(Future.successful(Right(role)))

      val resp = route(app, FakeRequest(POST, s"/roles?reactivate=true", jsonHeaders, jsonBody)).get

      val expectedJson =
        s"""
           |{"id":"${role.id}",
           |"name":"test_role",
           |"level":1,
           |"created_by":"${role.createdBy}",
           |"updated_by":"${role.updatedBy.get}",
           |"created_at":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr},
           |"created_time":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe CREATED

      contentAsString(resp) mustBe expectedJson
    }

    "get a role by id in GET /roles/:id" in {
      val role = Role(
        id = mockId,
        name = "test_role",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)
      val criteria = (mockId.some, none, none).asDomain

      (roleService.getActiveRolesByCriteria(
        _: Option[RoleCriteria],
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(Some(criteria), Seq.empty[Ordering], None, None).returns(Future.successful(Right(Seq(role))))

      val resp = route(app, FakeRequest(GET, s"/roles/$mockId")).get
      val expectedJson =
        s"""
           |{"id":"${role.id}",
           |"name":"test_role",
           |"level":1,
           |"created_by":"${role.createdBy}",
           |"updated_by":"${role.updatedBy.get}",
           |"created_at":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr},
           |"created_time":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "get all roles in GET /roles" in {
      val role = Role(
        id = mockId,
        name = "test_role",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)
      val criteria: RoleCriteria = (none, none, none).asDomain

      (roleService.countActiveRolesByCriteria _).when(Some(criteria)).returns(Future.successful(Right(1)))
      (latestVersionService.getLatestVersion _).when(criteria).returns(Future.successful(Right(role.updatedAt.map(_.toString))))
      (roleService.getActiveRolesByCriteria(
        _: Option[RoleCriteria],
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(Some(criteria), Seq.empty[Ordering], None, None).returns(Future.successful(Right(Seq(role))))

      val resp = route(app, FakeRequest(GET, s"/roles")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[{"id":"${role.id}",
           |"name":"test_role",
           |"level":1,
           |"created_by":"${role.createdBy}",
           |"updated_by":"${role.updatedBy.get}",
           |"created_at":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr},
           |"created_time":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}}],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "get X-Version with empty results in HEAD /roles" in {
      val mockLatestVersion = Some("")
      val criteria: RoleCriteria = (none, none, none).asDomain

      (roleService.countActiveRolesByCriteria _).when(Some(criteria)).returns(Future.successful(Right(0)))
      (latestVersionService.getLatestVersion _).when(criteria).returns(Future.successful(Right(None)))
      (roleService.getActiveRolesByCriteria(
        _: Option[RoleCriteria],
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(Some(criteria), Seq.empty[Ordering], None, None).returns(Future.successful(Right(Seq.empty)))

      val resp = route(app, FakeRequest(HEAD, s"/roles")).get
      val expectedJson =
        s"""
           |{"total":0,
           |"results":[],
           |"limit":null,
           |"offset":null}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
      headers(resp).get(versionHeaderKey) mustBe mockLatestVersion
    }

    "get paginated roles in GET /roles?limit=2&offset=2" in {
      val role = Role(
        id = mockId,
        name = "test_role",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)
      val criteria: RoleCriteria = (none, none, none).asDomain

      (roleService.countActiveRolesByCriteria _).when(Some(criteria)).returns(Future.successful(Right(1)))
      (latestVersionService.getLatestVersion _).when(criteria).returns(Future.successful(Right(role.updatedAt.map(_.toString))))
      (roleService.getActiveRolesByCriteria(
        _: Option[RoleCriteria],
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int])).when(Some(criteria), Seq.empty[Ordering], Some(2), Some(2)).returns(Future.successful(Right(Seq(role))))

      val resp = route(app, FakeRequest(GET, s"/roles?limit=2&offset=2")).get
      val expectedJson =
        s"""
           |{"total":1,
           |"results":[{"id":"${role.id}",
           |"name":"test_role",
           |"level":1,
           |"created_by":"${role.createdBy}",
           |"updated_by":"${role.updatedBy.get}",
           |"created_at":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr},
           |"created_time":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}}],
           |"limit":2,
           |"offset":2}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "update role by id in PUT /roles/:id" in {
      val jsonBody = s"""{"name": "test_role_update"}""".stripMargin
      val roleToUpdate = RoleToUpdate(
        name = Some("test_role_update"),
        level = None,
        lastUpdatedAt = None).asDomain(mockRequestDate, mockRequestFrom)
      val role = Role(
        id = mockId,
        name = "test_role_update",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)

      (roleService.updateRole _).when(mockId, roleToUpdate).returns(Future.successful(Right(role)))

      val resp = route(app, FakeRequest(PUT, s"/roles/$mockId", jsonHeaders, jsonBody)).get
      val expectedJson =
        s"""
           |{"id":"${role.id}",
           |"name":"test_role_update",
           |"level":1,
           |"created_by":"${role.createdBy}",
           |"updated_by":"${role.updatedBy.get}",
           |"created_at":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_at":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr},
           |"created_time":${role.createdAt.toZonedDateTimeUTC.toJsonStr},
           |"updated_time":${role.updatedAt.map(_.toZonedDateTimeUTC).toJsonStr}
           |}
         """.stripMargin.trim.replaceAll("\n", "")

      status(resp) mustBe OK

      contentAsString(resp) mustBe expectedJson
    }

    "delete a role by id in DELETE /roles/:id" in {
      val fakeLastUpdateAt = ZonedDateTime.now()
      val jsonBody =
        s"""{
           |"updated_at": "$fakeLastUpdateAt"
           |}""".stripMargin
      val role = Role(
        id = mockId,
        name = "test_role_update",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)

      (roleService.removeRole(
        _: UUID,
        _: String,
        _: LocalDateTime,
        _: Option[LocalDateTime])).when(mockId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, Some(fakeLastUpdateAt.toLocalDateTimeUTC)).returns(Future.successful(Right(role)))

      val resp = route(app, FakeRequest(DELETE, s"/roles/$mockId", jsonHeaders, jsonBody)).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe mockId.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }

    "delete a role by id in DELETE /roles/:id even without json payload for backwards compatibility" in {
      val jsonBody = ""
      val role = Role(
        id = mockId,
        name = "test_role_update",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = mockRequestFrom.some,
        updatedAt = LocalDateTime.now(mockClock).some)

      (roleService.removeRole(
        _: UUID,
        _: String,
        _: LocalDateTime,
        _: Option[LocalDateTime])).when(mockId, mockRequestFrom, mockRequestDate.toLocalDateTimeUTC, None)
        .returns(Future.successful(Right(role)))

      val resp = route(app, FakeRequest(DELETE, s"/roles/$mockId", jsonHeaders, jsonBody)).get

      //TODO unify delete responses to NO_CONTENT later when front-end catches up with these compatibility changes
      status(resp) mustBe OK
      contentAsString(resp) mustBe mockId.toJsonStr
      //status(resp) mustBe NO_CONTENT
    }
  }
}
