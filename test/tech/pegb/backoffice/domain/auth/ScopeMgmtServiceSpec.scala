package tech.pegb.backoffice.domain.auth

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.auth.abstraction.{PermissionDao, ScopeDao}
import tech.pegb.backoffice.dao.auth.entity.Scope
import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.dao.{DaoError, auth}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.abstraction.ScopeManagement
import tech.pegb.backoffice.domain.auth.dto.{PermissionCriteria, ScopeCriteria, ScopeToCreate, ScopeToUpdate}
import tech.pegb.backoffice.mapping.dao.domain.auth.scope.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.scope.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.permission.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class ScopeMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val scopeDao = stub[ScopeDao]
  private val permissionDao = stub[PermissionDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[ScopeDao].to(scopeDao),
      bind[PermissionDao].to(permissionDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val scopeMgmtService = inject[ScopeManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "ScopeMgmtService" should {
    "return created scope in createScope" in {
      val dto = ScopeToCreate(
        name = "report_scope",
        parentId = None,
        description = "Some description for report".some,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = Scope(
        id = UUID.randomUUID().toString,
        parentId = None,
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      (scopeDao.insertScope _)
        .when(dto.asDao)
        .returns(expected.asRight[DaoError])

      val result = scopeMgmtService.createScope(dto, false)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain.get)
      }
    }
    "return created scope in createScope with parent_id" in {
      val createDto = ScopeToCreate(
        name = "report_scope",
        parentId = UUID.randomUUID().some,
        description = "Some description for report".some,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = Scope(
        id = UUID.randomUUID().toString,
        parentId = createDto.parentId.map(_.toString),
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val expectedParent = Scope(
        id = createDto.parentId.get.toString,
        parentId = none,
        name = "accounts parent",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      (scopeDao.insertScope _)
        .when(createDto.asDao)
        .returns(expected.asRight[DaoError])

      val scopeCriteria = dto.ScopeCriteria(id = UUIDLike(expectedParent.id).some)
      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(isActive = true), None, None, None)
        .returns(Seq(expectedParent).asRight[DaoError])

      val result = scopeMgmtService.createScope(createDto, false)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain.get)
      }
    }
    "return error parent scope of scope to create does not exist" in {
      val parentScopeId = UUID.randomUUID()
      val createDto = ScopeToCreate(
        name = "report_scope",
        parentId = parentScopeId.some,
        description = "Some description for report".some,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = Scope(
        id = UUID.randomUUID().toString,
        parentId = createDto.parentId.map(_.toString),
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val scopeCriteria = dto.ScopeCriteria(id = UUIDLike(parentScopeId.toString).some)
      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(isActive = true), None, None, None)
        .returns(Nil.asRight[DaoError])

      val result = scopeMgmtService.createScope(createDto, false)
      whenReady(result) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"Parent Scope with id $parentScopeId doesn't exist"))
      }
    }
    "return reactivated scope in createScope" in {
      val creteDto = ScopeToCreate(
        name = "report_scope",
        parentId = None,
        description = "Some description for report".some,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = Scope(
        id = UUID.randomUUID().toString,
        parentId = None,
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val scopeCriteria = dto.ScopeCriteria(name = creteDto.name.some)

      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(isActive = false), None, None, None)
        .returns(Seq(expected).asRight[DaoError])
      (scopeDao.updateScope _)
        .when(expected.id, creteDto.asReactivateDao(expected.updatedAt))
        .returns(expected.some.asRight[DaoError])

      val result = scopeMgmtService.createScope(creteDto, true)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain.get)
      }
    }
    "return created scope when trying to reactivate a non existing scope" in {
      val createDto = ScopeToCreate(
        name = "report_scope",
        parentId = UUID.randomUUID().some,
        description = "Some description for report".some,
        createdAt = now,
        createdBy = "pegbuser")

      val expected = Scope(
        id = UUID.randomUUID().toString,
        parentId = createDto.parentId.map(_.toString),
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val expectedParent = Scope(
        id = createDto.parentId.get.toString,
        parentId = none,
        name = "accounts parent",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val scopeCriteria = dto.ScopeCriteria(name = createDto.name.some)

      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(isActive = false), None, None, None)
        .returns(Nil.asRight[DaoError])

      (scopeDao.insertScope _)
        .when(createDto.asDao)
        .returns(expected.asRight[DaoError])

      val parentScopeCriteria = dto.ScopeCriteria(id = UUIDLike(expectedParent.id).some)
      (scopeDao.getScopeByCriteria _)
        .when(parentScopeCriteria.asDao(isActive = true), None, None, None)
        .returns(Seq(expectedParent).asRight[DaoError])

      val result = scopeMgmtService.createScope(createDto, true)
      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain.get)
      }
    }
    "return scope matching id in getScopeById" in {

      val id = UUID.randomUUID()
      val scopeCriteria = dto.ScopeCriteria(id = UUIDLike(id.toString).some)

      val expected = Scope(
        id = id.toString,
        parentId = None,
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(), None, None, None)
        .returns(Seq(expected).asRight[DaoError])

      val result = scopeMgmtService.getScopeById(id)

      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain.get)
      }
    }
    "return scopes matching criteria in getScopeByCriteria" in {
      val id = UUID.randomUUID()
      val scopeCriteria = dto.ScopeCriteria(id = UUIDLike(id.toString).some)

      val expected = Scope(
        id = id.toString,
        parentId = None,
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(), None, None, None)
        .returns(Seq(expected).asRight[DaoError])

      val result = scopeMgmtService.getScopeById(id)

      whenReady(result) { actual ⇒
        actual mustBe Right(expected.asDomain.get)
      }
    }
    "return count of scopes matching criteria in countScopeByCriteria" in {
      val scopeCriteria = dto.ScopeCriteria()

      val s1 = Scope(
        id = UUID.randomUUID().toString,
        parentId = None,
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val s2 = Scope(
        id = UUID.randomUUID().toString,
        parentId = None,
        name = "fee_profile",
        description = "fee_profile scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val s3 = Scope(
        id = UUID.randomUUID().toString,
        parentId = s2.id.some,
        name = "fee_profile_update",
        description = "fee_profile_update scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      val expected = Seq(s1, s2, s3)

      (scopeDao.getScopeByCriteria _)
        .when(scopeCriteria.asDao(), None, None, None)
        .returns(expected.asRight[DaoError])

      val result = scopeMgmtService.getScopeByCriteria(scopeCriteria, Nil, None, None)

      whenReady(result) { actual ⇒
        actual mustBe Right(expected.map(_.asDomain.get))
      }
    }
    "return updated scope matching id updateScopeById" in {
      val id = UUID.randomUUID()
      val dto = ScopeToUpdate(
        description = "new description".some,
        updatedBy = "pegbuser",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val s1 = Scope(
        id = id.toString,
        parentId = None,
        name = "accounts",
        description = "new description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      (scopeDao.getScopeByCriteria _).when(ScopeCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(s1)))

      (scopeDao.updateScope _)
        .when(s1.id, dto.asDao(isActive = true.some, s1.updatedAt))
        .returns(s1.some.asRight[DaoError])

      val result = scopeMgmtService.updateScopeById(id, dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(s1.asDomain.get)
      }
    }
    "deleted scope in deleteScopeById" in {
      val id = UUID.randomUUID()

      val dto = ScopeToUpdate(
        updatedBy = "pegbuser",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val s1 = Scope(
        id = id.toString,
        parentId = None,
        name = "accounts",
        description = "new description".some,
        isActive = 0,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)

      (permissionDao.getPermissionByCriteria _)
        .when(PermissionCriteria(
          scopeId = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None)
        .returns(Nil.asRight[DaoError])

      (scopeDao.getScopeByCriteria _).when(ScopeCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None)
        .returns(Right(Seq(s1)))

      (scopeDao.updateScope _)
        .when(id.toString, dto.asDao(isActive = false.some, s1.updatedAt))
        .returns(s1.some.asRight[DaoError])

      val result = scopeMgmtService.deleteScopeById(id, now, "pegbuser", now.some)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }
  }

}
