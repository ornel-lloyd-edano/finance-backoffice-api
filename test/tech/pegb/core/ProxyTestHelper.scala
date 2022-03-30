package tech.pegb.core

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalatest.Suite
import tech.pegb.backoffice.domain.auth.dto.PermissionKeys.BusinessUnitAndRolePermissionKey
import tech.pegb.backoffice.domain.auth.model._

trait ProxyTestHelper extends PegBTestApp { this: Suite ⇒
  //=======================================================
  /*
   *       HELPER Objects
   */
  //=======================================================
  val fakeClock: Clock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val fakeNow = LocalDateTime.now(fakeClock)

  val testRole = Role(
    id = UUID.randomUUID(),
    name = "Admin",
    level = 0,
    createdBy = "pegbuser",
    createdAt = fakeNow,
    updatedBy = "pegbuser".some,
    updatedAt = fakeNow.some)

  val testBusinessUnit = BusinessUnit(
    id = UUID.randomUUID(),
    name = "Finance",
    createdBy = "pegbuser",
    updatedBy = "pegbuser".some,
    createdAt = fakeNow,
    updatedAt = fakeNow.some)

  def createTestBackOfficeUser(
    hashedPassword: Option[String],
    scopeName: String,
    businessUnit: Option[BusinessUnit] = None,
    role: Option[Role] = None): BackOfficeUser = {

    val roleToUse = role.getOrElse(testRole)
    val buToUse = businessUnit.getOrElse(testBusinessUnit)

    BackOfficeUser(
      id = UUID.randomUUID(),
      userName = "scala.user",
      hashedPassword = hashedPassword,
      role = roleToUse,
      businessUnit = buToUse,
      permissions = createPermissionsList(scopeName, buToUse.id, roleToUse.id),
      email = Email("scala.user@gmail.com"),
      phoneNumber = "+971555555".some,
      firstName = "Martin",
      middleName = None,
      lastName = "Odersky",
      description = None,
      homePage = None,
      activeLanguage = "en".some,
      customData = None,
      lastLoginTimestamp = None,
      createdBy = "pegbuser",
      createdAt = fakeNow,
      updatedBy = "pegbuser".some,
      updatedAt = fakeNow.some)
  }

  private def createPermissionsList(
    scopeName: String,
    buId: UUID,
    roleId: UUID): Seq[Permission] = {

    val parentScope = Scope(
      id = UUID.randomUUID(),
      parentId = None,
      name = scopeName,
      description = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val createScope = Scope(
      id = UUID.randomUUID(),
      parentId = parentScope.id.some,
      name = s"${scopeName}_create",
      description = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val detailScope = Scope(
      id = UUID.randomUUID(),
      parentId = parentScope.id.some,
      name = s"${scopeName}_detail",
      description = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val updateScope = Scope(
      id = UUID.randomUUID(),
      parentId = parentScope.id.some,
      name = s"${scopeName}_edit",
      description = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val deleteScope = Scope(
      id = UUID.randomUUID(),
      parentId = parentScope.id.some,
      name = s"${scopeName}_delete",
      description = None,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val parentPermission = Permission(
      id = UUID.randomUUID(),
      permissionKey = BusinessUnitAndRolePermissionKey(buId, roleId),
      scope = parentScope,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val createPermission = Permission(
      id = UUID.randomUUID(),
      permissionKey = BusinessUnitAndRolePermissionKey(buId, roleId),
      scope = createScope,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val detailPermission = Permission(
      id = UUID.randomUUID(),
      permissionKey = BusinessUnitAndRolePermissionKey(buId, roleId),
      scope = detailScope,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val updatePermission = Permission(
      id = UUID.randomUUID(),
      permissionKey = BusinessUnitAndRolePermissionKey(buId, roleId),
      scope = updateScope,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    val deletePermission = Permission(
      id = UUID.randomUUID(),
      permissionKey = BusinessUnitAndRolePermissionKey(buId, roleId),
      scope = deleteScope,
      createdBy = "pegbuser",
      updatedBy = "pegbuser".some,
      createdAt = fakeNow,
      updatedAt = fakeNow.some)

    Seq(parentPermission, createPermission, detailPermission, updatePermission, deletePermission)
  }

}
