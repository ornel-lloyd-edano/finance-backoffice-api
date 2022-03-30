package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.auth.abstraction.PermissionDao
import tech.pegb.backoffice.dao.auth.dto.{PermissionCriteria, PermissionToInsert, PermissionToUpdate}
import tech.pegb.backoffice.dao.auth.entity.{Permission, Scope}
import tech.pegb.backoffice.dao.model.{CriteriaField, Ordering, OrderingSet}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class PermissionDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[PermissionDao]

  val scopeId1 = "d21cfc70-d2b0-11e8-bcd3-000c291e73b1"
  val scopeId2 = "5c1d5483-e603-4395-9697-c95a35d8213b"
  val scopeId3 = "89841ce5-8905-4d10-b39f-fa277a4190fe"

  val permissionId1 = "c4b8b9ac-af50-4f38-8bc6-1e051d042735"
  val permissionId2 = "ce0811fa-f447-48e1-b947-64941674f4b0"
  val permissionId3 = "deaa4d37-2e06-4337-a58e-1f8e1e04e7b2"

  override def initSql =
    s"""
       |INSERT INTO business_units (id,`name`,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('fcb65b36-d282-41d4-b566-1ec641fc464b','asdasd',0,'system','system','$now','$now'),
       |('09864604-2caf-4b3f-a79c-c841105f580b','STRING',1,'system','Narek','$now','$now'),
       |('85bc6f6e-81a3-4e9a-a282-da649cfb427c','super-admin',1,'system','system','$now','$now'),
       |('c2f0cec1-16c5-4fb1-96ff-8167670aad77','TEST1',1,'system','system','$now','$now');
       |
       |INSERT INTO roles (id,`name`,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('4eba8b9b-c43f-4122-976d-c6d76d77890a','role1',0,'system','system','$now','$now'),
       |('12ec03f3-d1dc-11e8-bcd3-000c291e73b1','admin',1,'system','system','$now','$now'),
       |('047ddb43-3bc8-4b4d-ae62-822c2a08e49a','super_admin',1,'system','system','$now','$now');
       |
       |INSERT INTO back_office_users (id,userName,password,roleId,businessUnitId,email,phoneNumber,firstName,middleName,lastName,description,homePage,is_active,activeLanguage,customData,lastLoginTimestamp,created_at,updated_at,created_by,updated_by) VALUES
       |('9ffefdbd-d1dc-11e8-bcd3-000c291e73b1','pegbuser','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B','12ec03f3-d1dc-11e8-bcd3-000c291e73b1','09864604-2caf-4b3f-a79c-c841105f580b','pegbuser@pegb.tech','971557200221','pegbuser',NULL,'pegbuser','some description test','https://pegb.tech',1,NULL,NULL,1546940581250,'$now','$now','system','system'),
       |('6ebfa18c-d21d-11e8-bcd3-000c291e73b1','superadmin','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B','047ddb43-3bc8-4b4d-ae62-822c2a08e49a','85bc6f6e-81a3-4e9a-a282-da649cfb427c','superadmin@pegb.tech','97123456789','super',NULL,'admin','super description test','https://pegb.tech',1,NULL,NULL,1546940581250,'$now','$now','system','system'),
       |('1d4be6e3-f11e-4a9d-9e78-8a352f03e7a2','Narek','4A0F346D7A83912ED6E28BEC4E3014FA8F242D745620ACE9D930B287288C529B','12ec03f3-d1dc-11e8-bcd3-000c291e73b1','09864604-2caf-4b3f-a79c-c841105f580b','n.hakobyan@pegb.tech','095455551','NAREK',NULL,'HAKOBYAN','some description test','https://pegb.tech',1,'EN','{"mycustomvalue":10,"oneMore_field":"qwerty"}',1545311023468,'$now','$now','system','system');
       |
       |INSERT INTO scopes (id,parentId,`name`,description,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('$scopeId1',NULL,'accounts','scope description',1,'system','system','$now','$now'),
       |('$scopeId2',NULL,'back_office_users','scope description',1,'system','system','$now','$now'),
       |('$scopeId3',NULL,'business_units','General access to business units management',1,'system','system','$now','$now');
       |
       |INSERT INTO permissions (id,buId,userId,roleId,scopeId,canWrite,is_active,created_by,updated_by,created_at,updated_at) VALUES
       |('$permissionId1',NULL,'6ebfa18c-d21d-11e8-bcd3-000c291e73b1',NULL,'$scopeId1',1,1,'system','system','$now','$now'),
       |('$permissionId2',NULL,'6ebfa18c-d21d-11e8-bcd3-000c291e73b1',NULL,'$scopeId2',1,1,'system','system','$now','$now'),
       |('$permissionId3','fcb65b36-d282-41d4-b566-1ec641fc464b',NULL,'4eba8b9b-c43f-4122-976d-c6d76d77890a','$scopeId3',1,1,'system','system','$now','$now');
     """.stripMargin

  "PermissionDao Positive tests" should {
    val scope1 = Scope(
      id = scopeId1,
      parentId = None,
      name = "accounts",
      description = "scope description".some,
      isActive = 1,
      createdBy = Some("system"),
      createdAt = Some(now),
      updatedBy = "system".some,
      updatedAt = now.some)

    val scope2 = Scope(
      id = scopeId2,
      parentId = None,
      name = "back_office_users",
      description = "scope description".some,
      isActive = 1,
      createdBy = Some("system"),
      createdAt = Some(now),
      updatedBy = "system".some,
      updatedAt = now.some)

    val scope3 = Scope(
      id = scopeId3,
      parentId = None,
      name = "business_units",
      description = "General access to business units management".some,
      isActive = 1,
      createdBy = Some("system"),
      createdAt = Some(now),
      updatedBy = "system".some,
      updatedAt = now.some)

    val permission1 = Permission(
      id = permissionId1,
      businessUnitId = none,
      roleId = none,
      userId = "6ebfa18c-d21d-11e8-bcd3-000c291e73b1".some,
      scope = scope1,
      isActive = 1,
      createdBy = Some("system"),
      createdAt = Some(now),
      updatedBy = "system".some,
      updatedAt = now.some)

    val permission2 = Permission(
      id = permissionId2,
      businessUnitId = none,
      roleId = none,
      userId = "6ebfa18c-d21d-11e8-bcd3-000c291e73b1".some,
      scope = scope2,
      isActive = 1,
      createdBy = Some("system"),
      createdAt = Some(now),
      updatedBy = "system".some,
      updatedAt = now.some)

    val permission3 = Permission(
      id = permissionId3,
      businessUnitId = "fcb65b36-d282-41d4-b566-1ec641fc464b".some,
      roleId = "4eba8b9b-c43f-4122-976d-c6d76d77890a".some,
      userId = none,
      scope = scope3,
      isActive = 1,
      createdBy = Some("system"),
      createdAt = Some(now),
      updatedBy = "system".some,
      updatedAt = now.some)

    "return permission seq in getPermissionByCriteria no filter" in {
      val criteria = PermissionCriteria()
      val orderingSet = OrderingSet(Ordering("id", Ordering.ASC)).some

      val result = dao.getPermissionByCriteria(criteria, orderingSet, None, None)

      result mustBe Right(Seq(permission1, permission2, permission3))
    }
    "return permission seq in getPermissionByCriteria filter by id " in {
      val criteria = PermissionCriteria(id = CriteriaField("", permissionId1).some)

      val result = dao.getPermissionByCriteria(criteria, None, None, None)

      result mustBe Right(Seq(permission1))
    }
    "return permission seq in getPermissionByCriteria filter by buId " in {
      val criteria = PermissionCriteria(businessId = CriteriaField("", "fcb65b36-d282-41d4-b566-1ec641fc464b").some)

      val result = dao.getPermissionByCriteria(criteria, None, None, None)

      result mustBe Right(Seq(permission3))
    }
    "return permission seq in getPermissionByCriteria filter by roleId " in {
      val criteria = PermissionCriteria(roleId = CriteriaField("", "4eba8b9b-c43f-4122-976d-c6d76d77890a").some)

      val result = dao.getPermissionByCriteria(criteria, None, None, None)

      result mustBe Right(Seq(permission3))
    }
    "return permission seq in getPermissionByCriteria filter by scopeId " in {
      val criteria = PermissionCriteria(scopeId = CriteriaField("", scopeId2).some)

      val result = dao.getPermissionByCriteria(criteria, None, None, None)

      result mustBe Right(Seq(permission2))
    }
    "return count in countPermissionByCriteria" in {
      val criteria = PermissionCriteria()

      val result = dao.countPermissionByCriteria(criteria)

      result mustBe Right(3)
    }
    "return count in countPermissionByCriteria filter by scopeId" in {
      val criteria = PermissionCriteria(scopeId = CriteriaField("", scopeId2).some)

      val result = dao.countPermissionByCriteria(criteria)

      result mustBe Right(1)
    }
    "return created permission in createPermission" in {
      val dto = PermissionToInsert(
        businessUnitId = "85bc6f6e-81a3-4e9a-a282-da649cfb427c".some,
        roleId = "12ec03f3-d1dc-11e8-bcd3-000c291e73b1".some,
        userId = none,
        canWrite = 1.some,
        isActive = 1.some,
        scopeId = scopeId1,
        createdAt = now,
        createdBy = "pegbuser")

      val resp = dao.insertPermission(dto)

      val permission = resp.right.get

      permission.businessUnitId mustBe dto.businessUnitId
      permission.roleId mustBe dto.roleId
      permission.userId mustBe dto.userId
      permission.scope mustBe scope1
      permission.isActive mustBe dto.isActive.get
      permission.createdAt mustBe dto.createdAt.some
      permission.createdBy mustBe dto.createdBy.some
      permission.updatedAt mustBe dto.createdAt.some
      permission.updatedBy mustBe dto.createdBy.some
    }
    "return updated permission in updatePermissionById" in {
      val dto = PermissionToUpdate(
        businessUnitId = "85bc6f6e-81a3-4e9a-a282-da649cfb427c".some,
        roleId = "12ec03f3-d1dc-11e8-bcd3-000c291e73b1".some,
        userId = none,
        scopeId = scopeId2.some,
        isActive = 0.some,
        updatedBy = "george",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val resp = dao.updatePermission(permissionId3, dto)

      resp mustBe Right(
        permission3.copy(
          businessUnitId = dto.businessUnitId,
          roleId = dto.roleId,
          isActive = 0,
          scope = scope2,
          updatedBy = dto.updatedBy.some,
          updatedAt = dto.updatedAt.some).some)
    }
    "returen precondition failed in deletePermissionById" in {
      val dto = PermissionToUpdate(
        businessUnitId = "85bc6f6e-81a3-4e9a-a282-da649cfb427c".some,
        roleId = "12ec03f3-d1dc-11e8-bcd3-000c291e73b1".some,
        userId = none,
        scopeId = scopeId2.some,
        isActive = 0.some,
        updatedBy = "george",
        updatedAt = now,
        lastUpdatedAt = LocalDateTime.now().some)

      val resp = dao.updatePermission(permissionId3, dto)

      resp mustBe Left(PreconditionFailed(s"Update failed. Permission $permissionId3 has been modified by another process."))

    }
  }
}

