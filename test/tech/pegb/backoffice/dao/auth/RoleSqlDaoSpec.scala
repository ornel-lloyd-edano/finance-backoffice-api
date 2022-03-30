package tech.pegb.backoffice.dao.auth

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import play.api.inject.Binding
import tech.pegb.backoffice.dao.auth.sql.RoleSqlDao._
import tech.pegb.backoffice.dao.auth.dto.{RoleCriteria, RoleToCreate, RoleToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Role
import tech.pegb.backoffice.dao.auth.sql.RoleSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

class RoleSqlDaoSpec extends PegBTestApp {

  override val additionalBindings: Seq[Binding[_]] = super.additionalBindings ++ Seq()

  private val mockClock = Clock.fixed(Instant.ofEpochMilli(1571293597000L), ZoneId.systemDefault())
  lazy val roleSqlDao: RoleSqlDao = fakeApplication().injector.instanceOf[RoleSqlDao]
  val mockId: UUID = UUID.randomUUID()

  override def initSql: String =
    s"""
       |INSERT INTO roles (id,`name`,is_active,level,created_by, created_at) VALUES
       |('4eba8b9b-c43f-4122-976d-c6d76d77890a','role1',0,1,'system','${LocalDateTime.now(mockClock)}'),
       |('12ec03f3-d1dc-11e8-bcd3-000c291e73b1','admin',1,1,'system','${LocalDateTime.now(mockClock)}'),
       |('047ddb43-3bc8-4b4d-ae62-822c2a08e49a','super_admin',0,1,'system','${LocalDateTime.now(mockClock)}');
     """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM roles;
     """.stripMargin

  "RoleSqlDao" should {
    "insert a new Role" in {
      val dto = RoleToCreate(
        id = mockId,
        name = "test_role",
        isActive = 1,
        level = 1,
        createdBy = "ujali",
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = "ujali",
        updatedAt = LocalDateTime.now(mockClock))

      val result = roleSqlDao.createRole(dto)

      val expected = Role(
        id = dto.id,
        name = dto.name,
        level = dto.level,
        isActive = 1,
        createdBy = Some(dto.createdBy),
        createdAt = Some(dto.createdAt),
        updatedBy = Some(dto.updatedBy),
        updatedAt = Some(dto.updatedAt))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "fail to insert a new Role if name is already existing" in {
      val mockId = UUID.randomUUID()
      val dto = RoleToCreate(
        id = mockId,
        name = "test_role",
        isActive = 1,
        level = 1,
        createdBy = "ujali",
        createdAt = LocalDateTime.now(mockClock),
        updatedBy = "ujali",
        updatedAt = LocalDateTime.now(mockClock))

      val result = roleSqlDao.createRole(dto)

      result.isLeft mustBe true
    }

    "update an existing Role" in {
      val idToUpdate = UUID.fromString("4eba8b9b-c43f-4122-976d-c6d76d77890a")
      val dto = RoleToUpdate(updatedBy = "ujali", updatedAt = LocalDateTime.now(mockClock), name = Some("super_admin_new"))

      val result = roleSqlDao.updateRole(idToUpdate, dto)

      val expected = Some(Role(
        id = idToUpdate,
        name = "super_admin_new",
        isActive = 0,
        level = 1,
        createdBy = Some("system"),
        createdAt = Some(LocalDateTime.now(mockClock)),
        updatedBy = Some("ujali"),
        updatedAt = Some(LocalDateTime.now(mockClock))))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "count Roles without criteria" in {

      val result = roleSqlDao.countRolesByCriteria(None)

      val expected = 4

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "count Roles from given criteria" in {

      val dto = RoleCriteria(name = Some(CriteriaField(cName, "test_role")))

      val result = roleSqlDao.countRolesByCriteria(Some(dto))

      val expected = 1

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "get Roles from given criteria" in {
      val dto = RoleCriteria(level = Some(CriteriaField(cLevel, 1)))

      val result = roleSqlDao.getRolesByCriteria(Some(dto), None, None, None)

      val expected = Set(
        Role(UUID.fromString("047ddb43-3bc8-4b4d-ae62-822c2a08e49a"), "super_admin", 1, 0, Some("system"), Some(LocalDateTime.now(mockClock)), None, None),
        Role(UUID.fromString("12ec03f3-d1dc-11e8-bcd3-000c291e73b1"), "admin", 1, 1, Some("system"), Some(LocalDateTime.now(mockClock)), None, None),
        Role(UUID.fromString("4eba8b9b-c43f-4122-976d-c6d76d77890a"), "super_admin_new", 1, 0, Some("system"), Some(LocalDateTime.now(mockClock)), Some("ujali"), Some(LocalDateTime.now(mockClock))),
        Role(mockId, "test_role", 1, 1, Some("ujali"), Some(LocalDateTime.now(mockClock)), Some("ujali"), Some(LocalDateTime.now(mockClock))))

      result.isRight mustBe true
      result.right.get.toSet mustBe expected
    }

    "get Roles from given criteria and ordering" in {
      val dto = RoleCriteria(name = Some(CriteriaField(cName, "test_role")))
      val ordering = OrderingSet(Ordering("id", Ordering.DESC))

      val result = roleSqlDao.getRolesByCriteria(Some(dto), Some(ordering), None, None)

      val expected = Seq(
        Role(mockId, "test_role", 1, 1, Some("ujali"), Some(LocalDateTime.now(mockClock)), Some("ujali"), Some(LocalDateTime.now(mockClock))))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "get Roles from given criteria, ordering and pagination" in {
      val criteria = RoleCriteria(level = Some(CriteriaField(cLevel, 1)))
      val ordering = OrderingSet(Ordering("name", Ordering.DESC))
      val limit = Some(1)
      val offset = Some(2)

      val result = roleSqlDao.getRolesByCriteria(Some(criteria), Some(ordering), limit, offset)

      val expected = Seq(Role(UUID.fromString("047ddb43-3bc8-4b4d-ae62-822c2a08e49a"), "super_admin", 1, 0, Some("system"), Some(LocalDateTime.now(mockClock)), None, None))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "get most recent updated Role" in {
      val criteria = RoleCriteria(level = Some(CriteriaField(cLevel, 1)))
      val result = roleSqlDao.getMostRecentUpdatedAt(Some(criteria))
      val expected = Some(
        Role(mockId, "test_role", 1, 1, Some("ujali"), Some(LocalDateTime.now(mockClock)), Some("ujali"), Some(LocalDateTime.now(mockClock))))

      result.isRight mustBe true
      result.right.get mustBe expected
    }
  }
}
