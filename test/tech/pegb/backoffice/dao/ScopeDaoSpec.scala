package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.auth.abstraction.ScopeDao
import tech.pegb.backoffice.dao.auth.dto.{ScopeCriteria, ScopeToInsert, ScopeToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Scope
import tech.pegb.backoffice.dao.model.{MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class ScopeDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[ScopeDao]

  val accountScopeId = UUID.randomUUID()
  val backOfficeUserParentId = UUID.randomUUID()
  val backOfficeUserScopeId = UUID.randomUUID()
  val businessUnitScopeId = UUID.randomUUID()

  override def initSql =
    s"""
       |INSERT INTO scopes (id,parentId,`name`,description,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('$accountScopeId',NULL,'accounts','scope description',1,'system','system','$now','$now'),
       |('$backOfficeUserParentId',NULL,'back_office_users_parent','scope description',1,'system','system','$now','$now'),
       |('$backOfficeUserScopeId','$backOfficeUserParentId','back_office_users','scope description yeah',1,'system','system','$now','$now'),
       |('$businessUnitScopeId',NULL,'business_units','General access to business units management',1,'system','system','$now','$now'),
     """.stripMargin

  "ScopeDao Positive tests" should {
    "return scope in getScopeByCriteria by Id if exist" in {

      val criteria = ScopeCriteria(
        id = model.CriteriaField("", accountScopeId.toString).some)

      val resp = dao.getScopeByCriteria(criteria, none, none, none)

      resp mustBe Right(Seq(Scope(
        id = accountScopeId.toString,
        parentId = None,
        name = "accounts",
        description = "scope description".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)))

    }
    "return scope in getScopeByCriteria by ParentId if exist" in {

      val criteria = ScopeCriteria(
        parentId = model.CriteriaField("", backOfficeUserParentId.toString).some)

      val resp = dao.getScopeByCriteria(criteria, none, none, none)

      resp mustBe Right(Seq(Scope(
        id = backOfficeUserScopeId.toString,
        parentId = backOfficeUserParentId.toString.some,
        name = "back_office_users",
        description = "scope description yeah".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)))

    }
    "return scope in getScopeByCriteria by name if exist" in {

      val criteria = ScopeCriteria(
        name = model.CriteriaField("", "back_office_users").some)

      val resp = dao.getScopeByCriteria(criteria, none, none, none)

      resp mustBe Right(Seq(Scope(
        id = backOfficeUserScopeId.toString,
        parentId = backOfficeUserParentId.toString.some,
        name = "back_office_users",
        description = "scope description yeah".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)))

    }
    "return scope in getScopeByCriteria by description if exist" in {

      val criteria = ScopeCriteria(
        description = model.CriteriaField("", "yeah", MatchTypes.Partial).some)

      val resp = dao.getScopeByCriteria(criteria, none, none, none)

      resp mustBe Right(Seq(Scope(
        id = backOfficeUserScopeId.toString,
        parentId = backOfficeUserParentId.toString.some,
        name = "back_office_users",
        description = "scope description yeah".some,
        isActive = 1,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "system".some,
        updatedAt = now.some)))

    }
    "return scope in getScopeByCriteria by isActive " in {

      val criteria = ScopeCriteria(
        isActive = model.CriteriaField("", 1).some)

      val orderingSet = OrderingSet(Ordering("name", Ordering.ASC)).some
      val resp = dao.getScopeByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(
        Scope(
          id = accountScopeId.toString,
          parentId = None,
          name = "accounts",
          description = "scope description".some,
          isActive = 1,
          createdBy = Some("system"),
          createdAt = Some(now),
          updatedBy = "system".some,
          updatedAt = now.some),
        Scope(
          id = backOfficeUserScopeId.toString,
          parentId = backOfficeUserParentId.toString.some,
          name = "back_office_users",
          description = "scope description yeah".some,
          isActive = 1,
          createdBy = Some("system"),
          createdAt = Some(now),
          updatedBy = "system".some,
          updatedAt = now.some),
        Scope(
          id = backOfficeUserParentId.toString,
          parentId = none,
          name = "back_office_users_parent",
          description = "scope description".some,
          isActive = 1,
          createdBy = Some("system"),
          createdAt = Some(now),
          updatedBy = "system".some,
          updatedAt = now.some),
        Scope(
          id = businessUnitScopeId.toString,
          parentId = none,
          name = "business_units",
          description = "General access to business units management".some,
          isActive = 1,
          createdBy = Some("system"),
          createdAt = Some(now),
          updatedBy = "system".some,
          updatedAt = now.some)))

    }
    "return List of scope in getByCriteria when criteria is empty and apply limit offset" in {
      val criteria = ScopeCriteria()

      val orderingSet = OrderingSet(Ordering("name", Ordering.ASC)).some
      val resp = dao.getScopeByCriteria(criteria, orderingSet, 2.some, 1.some)

      resp mustBe Right(Seq(
        Scope(
          id = backOfficeUserScopeId.toString,
          parentId = backOfficeUserParentId.toString.some,
          name = "back_office_users",
          description = "scope description yeah".some,
          isActive = 1,
          createdBy = Some("system"),
          createdAt = Some(now),
          updatedBy = "system".some,
          updatedAt = now.some),
        Scope(
          id = backOfficeUserParentId.toString,
          parentId = none,
          name = "back_office_users_parent",
          description = "scope description".some,
          isActive = 1,
          createdBy = Some("system"),
          createdAt = Some(now),
          updatedBy = "system".some,
          updatedAt = now.some)))

    }

    "return count in countScopeByCriteria" in {
      val criteria = ScopeCriteria()

      val resp = dao.countScopeByCriteria(criteria)

      resp mustBe Right(4)

    }
    "return created scope in insertScope" in {
      val dto = ScopeToInsert(
        name = "report_scope",
        parentId = None,
        description = "scope for report".some,
        isActive = 1,
        createdBy = "pegbuser",
        createdAt = now)

      val resp = dao.insertScope(dto)

      val scope = resp.right.get

      scope.name mustBe dto.name
      scope.parentId mustBe dto.parentId
      scope.description mustBe dto.description
      scope.isActive mustBe dto.isActive
      scope.createdAt mustBe dto.createdAt.some
      scope.createdBy mustBe dto.createdBy.some
      scope.updatedAt mustBe dto.createdAt.some
      scope.updatedBy mustBe dto.createdBy.some
    }
    "return updated scope in updateScopeById" in {

      val dto = ScopeToUpdate(
        description = "new description".some,
        isActive = 0.some,
        updatedBy = "george",
        updatedAt = LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(3601), ZoneId.systemDefault())),
        lastUpdatedAt = now.some)

      val resp = dao.updateScope(accountScopeId.toString, dto)

      resp mustBe Right(Scope(
        id = accountScopeId.toString,
        parentId = None,
        name = "accounts",
        description = "new description".some,
        isActive = 0,
        createdBy = Some("system"),
        createdAt = Some(now),
        updatedBy = "george".some,
        updatedAt = LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(3601), ZoneId.systemDefault())).some).some)
    }
    "return precondition failed scope in updateScopeById" in {

      val dto = ScopeToUpdate(
        description = "new description".some,
        isActive = 0.some,
        updatedBy = "george",
        updatedAt = LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(3601), ZoneId.systemDefault())),
        lastUpdatedAt = now.some)

      val resp = dao.updateScope(accountScopeId.toString, dto)

      resp mustBe Left(PreconditionFailed(s"Update failed. Scope $accountScopeId has been modified by another process."))

    }
  }
}
