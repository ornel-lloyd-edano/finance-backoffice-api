package tech.pegb.backoffice.domain.auth

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.{Binding, bind}
import tech.pegb.backoffice.domain.auth.dto.{BackOfficeUserCriteria, RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.dao.auth.abstraction.RoleDao
import tech.pegb.backoffice.domain.auth.abstraction.RoleService
import tech.pegb.backoffice.dao.auth.entity.Role
import tech.pegb.backoffice.domain.auth.model.{Role ⇒ DomainRole}
import tech.pegb.backoffice.domain.ErrorCodes
import tech.pegb.backoffice.mapping.domain.dao.auth.role.Implicits._
import tech.pegb.backoffice.domain.auth.abstraction.BackOfficeUserService
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class RoleServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockId: UUID = UUID.randomUUID()
  val mockClock: Clock = Clock.fixed(Instant.ofEpochMilli(1571646258000L), ZoneId.systemDefault())
  val roleDao: RoleDao = stub[RoleDao]
  val backOfficeUserService: BackOfficeUserService = stub[BackOfficeUserService]

  override def additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq(
      bind[RoleDao].to(roleDao),
      bind[BackOfficeUserService].to(backOfficeUserService),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val roleService: RoleService = inject[RoleService]

  "RoleService" should {
    "count active roles by criteria" in {
      val criteria = RoleCriteria(name = Some("test_role"), level = Some(1))

      (roleDao.countRolesByCriteria _).when(Some(criteria.asDao(Some(true)))).returns(Right(2))
      val result = roleService.countActiveRolesByCriteria(Some(criteria))

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe 2
      }
    }

    "get active roles by criteria" in {
      val criteria = RoleCriteria(name = Some("test_role"), level = Some(1))
      val daoRole = Role(
        id = mockId,
        name = "test_role",
        level = 1,
        isActive = 1,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some(mockRequestFrom),
        updatedAt = Some(LocalDateTime.now(mockClock)))
      val role = DomainRole(
        id = mockId,
        name = "test_role",
        level = 1,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = Some(mockRequestFrom),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(Some(true))), None, None, None).returns(Right(Seq(daoRole)))
      val result = roleService.getActiveRolesByCriteria(Some(criteria), Seq.empty, None, None)
      val expected = Seq(role)

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "create role" in {
      val criteria = RoleCriteria(name = Some("new_role"))
      val dto = RoleToCreate("new_role", level = 2, createdBy = "pegbuser", createdAt = LocalDateTime.now(mockClock))
      val daoRole = Role(
        id = mockId,
        name = "new_role",
        level = 2,
        isActive = 1,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some(mockRequestFrom),
        updatedAt = Some(LocalDateTime.now(mockClock)))
      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao()), None, None, None).returns(Right(Seq.empty))
      (roleDao.createRole _).when(*).returns(Right(daoRole))

      val result = roleService.createActiveRole(dto, reactivateIfExisting = false)

      val expected = DomainRole(id = mockId, name = dto.name, level = dto.level,
        createdBy = dto.createdBy, createdAt = dto.createdAt,
        updatedBy = Some(dto.createdBy), updatedAt = Some(dto.createdAt))

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get.copy(id = mockId) mustBe expected
      }
    }

    "recreate a deactivated role" in {
      val criteria = RoleCriteria(name = Some("new_role"))
      val dto = RoleToCreate("new_role", level = 2, createdBy = "pegbuser", createdAt = LocalDateTime.now(mockClock))
      val daoRole = Role(
        id = mockId,
        name = "new_role",
        level = 2,
        isActive = 0,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some(mockRequestFrom),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao()), None, None, None).returns(Right(Seq(daoRole)))

      (roleDao.updateRole _).when(mockId, RoleToUpdate(updatedAt = dto.createdAt, updatedBy = dto.createdBy).asDao(isActive = Some(true)))
        .returns(Right(Option(daoRole.copy(isActive = 1, level = 2))))

      val result = roleService.createActiveRole(dto, reactivateIfExisting = true)

      val expected = DomainRole(id = daoRole.id, name = dto.name, level = dto.level, createdBy = dto.createdBy,
        createdAt = dto.createdAt, updatedBy = Some(dto.createdBy), updatedAt = Some(dto.createdAt))
      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "fail to recreate a deactivated role if reactivate flag is set to false" in {
      val criteria = RoleCriteria(name = Some("new_role"))
      val dto = RoleToCreate("new_role", level = 2, createdBy = "pegbuser", createdAt = LocalDateTime.now(mockClock))
      val daoRole = Role(
        id = mockId,
        name = "new_role",
        level = 2,
        isActive = 0,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some(mockRequestFrom),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao()), None, None, None).returns(Right(Seq(daoRole)))

      val result = roleService.createActiveRole(dto, reactivateIfExisting = false)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.Duplicate
      }
    }

    "update a role by id" in {
      val criteria = RoleCriteria(name = Some("updated_role_new"))
      val dto = RoleToUpdate(name = Some("updated_role_new"), updatedBy = "pegbuser2", updatedAt = LocalDateTime.now(mockClock), lastUpdatedAt = None)

      val daoRole = Role(
        id = mockId,
        name = "updated_role_old",
        level = 2,
        isActive = 1,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))
      val daoRoleNew = Role(
        id = mockId,
        name = "updated_role_new",
        level = 2,
        isActive = 1,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(isActive = Some(true), butNotThisId = Some(mockId))), None, None, None).returns(Right(Seq(daoRole)))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(daoRole.copy(updatedAt = None))))
      (roleDao.updateRole _).when(mockId, dto.asDao(isActive = Some(true), maybeMissingLastUpdatedAt = None)).returns(Right(Some(daoRoleNew)))

      val result = roleService.updateRole(mockId, dto)

      val expected = DomainRole(
        id = mockId,
        name = "updated_role_new",
        level = 2,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "update a role by id when name is not provided" in {
      val criteria = RoleCriteria(name = None)
      val dto = RoleToUpdate(name = None, level = Some(3), updatedBy = "pegbuser2", updatedAt = LocalDateTime.now(mockClock), lastUpdatedAt = None)

      val daoRoleNew = Role(
        id = mockId,
        name = "updated_role_new",
        level = 3,
        isActive = 1,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(isActive = Some(true), butNotThisId = Some(mockId))), None, None, None).returns(Right(Seq.empty))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(daoRoleNew.copy(updatedAt = None))))
      (roleDao.updateRole _).when(mockId, dto.asDao(isActive = Some(true), maybeMissingLastUpdatedAt = None)).returns(Right(Some(daoRoleNew)))

      val result = roleService.updateRole(mockId, dto)

      val expected = DomainRole(
        id = mockId,
        name = "updated_role_new",
        level = 3,
        createdBy = mockRequestFrom,
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "fail to update a role by id if not found" in {
      val criteria = RoleCriteria(name = Some("updated_role"))
      val dto = RoleToUpdate(name = Some("updated_role"), updatedBy = "pegbuser2", updatedAt = LocalDateTime.now(mockClock), lastUpdatedAt = None)

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(isActive = Some(true), butNotThisId = Some(mockId))), None, None, None).returns(Right(Seq.empty))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(Role.empty.copy(updatedAt = None))))
      (roleDao.updateRole _).when(mockId, dto.asDao(isActive = Some(true), maybeMissingLastUpdatedAt = None)).returns(Right(None))

      val result = roleService.updateRole(mockId, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.NotFound
      }
    }

    "fail to update a role by id if name is already taken" in {
      val criteria = RoleCriteria(name = Some("taken_role_name"))
      val dto = RoleToUpdate(name = Some("taken_role_name"), updatedBy = "pegbuser2", updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0), lastUpdatedAt = None)

      val daoRole = Role(
        id = mockId,
        name = "taken_role_name",
        level = 2,
        isActive = 0,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(isActive = Some(true), butNotThisId = Some(mockId))), None, None, None).returns(Right(Seq(daoRole)))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(Role.empty.copy(updatedAt = None))))

      val result = roleService.updateRole(mockId, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.Duplicate
      }
    }

    "fail to update a role by id if level is less than zero" in {
      val criteria = RoleCriteria(name = None)
      val dto = RoleToUpdate(level = Some(-1), updatedBy = "pegbuser2", updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0), lastUpdatedAt = None)

      val daoRole = Role(
        id = mockId,
        name = "test_role",
        level = 2,
        isActive = 0,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(isActive = Some(true), butNotThisId = Some(mockId))), None, None, None).returns(Right(Seq(daoRole)))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(Role.empty.copy(updatedAt = None))))

      val result = roleService.updateRole(mockId, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.ValidationFailed
      }
    }

    "fail to update a role by id if level is greater than four" in {
      val criteria = RoleCriteria(name = None)
      val dto = RoleToUpdate(level = Some(5), updatedBy = "pegbuser2", updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0), lastUpdatedAt = None)

      val daoRole = Role(
        id = mockId,
        name = "test_role",
        level = 2,
        isActive = 0,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("pegbuser2"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (roleDao.getRolesByCriteria _).when(Some(criteria.asDao(isActive = Some(true), butNotThisId = Some(mockId))), None, None, None).returns(Right(Seq(daoRole)))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(Role.empty.copy(updatedAt = None))))

      val result = roleService.updateRole(mockId, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.ValidationFailed
      }
    }

    "deactivate a role by id" in {
      val backOfficeCriteria = dto.BackOfficeUserCriteria(roleId = Some(UUIDLike(mockId.toString)))
      val roleToUpdate = RoleToUpdate(updatedBy = "ujali", updatedAt = LocalDateTime.now(mockClock), lastUpdatedAt = None)

      val daoRole = Role(
        id = mockId,
        name = "updated_role_new",
        level = 2,
        isActive = 0,
        createdBy = Some(mockRequestFrom),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("ujali"),
        updatedAt = Some(LocalDateTime.now(mockClock)))

      (backOfficeUserService.getActiveBackOfficeUsersByCriteria(
        _: Option[BackOfficeUserCriteria],
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int]))
        .when(Some(backOfficeCriteria), Seq.empty[Ordering], None, None).
        returns(Future.successful(Right(Seq.empty)))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq(Role.empty.copy(updatedAt = None))))
      (roleDao.updateRole _).when(mockId, roleToUpdate.asDao(isActive = Some(false), maybeMissingLastUpdatedAt = None)).returns(Right(Some(daoRole)))

      val result = roleService.removeRole(mockId, "ujali", LocalDateTime.now(mockClock), None)

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe ()
      }
    }

    "fail to deactivate a role by id if not found" in {

      val backOfficeCriteria = dto.BackOfficeUserCriteria(roleId = Some(UUIDLike(mockId.toString)))
      val roleToUpdate = RoleToUpdate(updatedBy = "ujali", updatedAt = LocalDateTime.now(mockClock), lastUpdatedAt = None)

      (backOfficeUserService.getActiveBackOfficeUsersByCriteria(
        _: Option[BackOfficeUserCriteria],
        _: Seq[Ordering],
        _: Option[Int],
        _: Option[Int]))
        .when(Some(backOfficeCriteria), Seq.empty[Ordering], None, None).
        returns(Future.successful(Right(Seq.empty)))
      (roleDao.getRolesByCriteria _).when(Some(RoleCriteria(id = Some(mockId)).asDao(isActive = Some(true))), None, None, None).returns(Right(Seq()))
      (roleDao.updateRole _).when(mockId, roleToUpdate.asDao(isActive = Some(false), maybeMissingLastUpdatedAt = None)).returns(Right(None))

      val result = roleService.removeRole(mockId, "ujali", LocalDateTime.now(mockClock), None)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.NotFound
      }
    }
  }
}
