package tech.pegb.backoffice

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import play.api.test.Helpers.AUTHORIZATION
import tech.pegb.backoffice.api.proxy.model.Scope
import tech.pegb.backoffice.dao.auth.abstraction._
import tech.pegb.backoffice.dao.auth.dto._
import tech.pegb.backoffice.domain.auth.abstraction.TokenService
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.model.ClaimContent
import tech.pegb.backoffice.util.Logging
import tech.pegb.core.PegBTestAppWithServer
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._

trait SuperAdminCreator extends PegBTestAppWithServer with Logging {

  var superToken: String = _

  val now = LocalDateTime.now()
  val scopesList = List(
    Scope("currency_exchanges"),
    Scope("fee_profiles"),
    Scope("individual_users"),
    Scope("transactions"),
    Scope("accounts"),
    Scope("floats"),
    Scope("currency_rates"),
    Scope("limit_profiles"),
    Scope("documents"),
    Scope("notification_templates"),
    Scope("spreads"),
    Scope("tasks"),
    Scope("strings"),
    Scope("wallet_applications"),
    Scope("manual_transactions"),
    Scope("routes"),
    Scope("reporting"),
    Scope("report_definitions"),
    Scope("business_user_applications"),
    Scope("dashboards"),
    Scope("business_users"),
    Scope("external_accounts")
  ).distinct

  implicit val tokenExpirationInMinutes = TokenExpiration(90)

  def AuthHeader: (String, String) = (AUTHORIZATION, s"Bearer $superToken")

  protected def createSuperAdmin(): Unit = {
    val buDao = inject[BusinessUnitDao]
    val roleDao = inject[RoleDao]
    val backOfficeUserDao = inject [BackOfficeUserDao]
    val tokenService: TokenService = inject[TokenService]

    for {
      bu ← buDao.create(BusinessUnitToInsert(
        name = "super_unit",
        isActive = 1,
        createdBy = "integration_test",
        createdAt = now,
        updatedBy = "integration_test".some,
        updatedAt = now.some))
      role ← roleDao.createRole(RoleToCreate(
        id = UUID.randomUUID(),
        name = "super_role",
        isActive = 1,
        level = 0,
        createdBy = "integration_test",
        createdAt = now,
        updatedBy = "integration_test",
        updatedAt = now))
      user ← backOfficeUserDao.createBackOfficeUser(BackOfficeUserToInsert(
        userName = "superuser",
        password = "Abc12345678*".some,
        email = "superuser@pegb.tech",
        phoneNumber = None,
        firstName = "Kira",
        middleName = None,
        lastName = "Ward",
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        lastLoginTimestamp = None,
        roleId = role.id.toString,
        businessUnitId = bu.id,
        isActive = 1,
        createdBy = "integration_test",
        createdAt = now,
        updatedBy = "integration_test".some,
        updatedAt = now.some
      ))
      userDomain ← user.asDomain.toEither
    } yield {
      prepareScopesAndPermission(bu.id, role.id.toString)
      val content = ClaimContent.from(userDomain)
      superToken = tokenService.generateToken("superuser", content)
    }
  }

  protected def prepareScopesAndPermission(buId: String, roleId: String): Unit = {
    val scopeDao = inject[ScopeDao]
    val permissionDao = inject[PermissionDao]

    def createPermission(scopeId: String) = {
      permissionDao.insertPermission(PermissionToInsert(
        businessUnitId = buId.some,
        roleId = roleId.some,
        userId = None,
        canWrite = 1.some,
        isActive = 1.some,
        scopeId = scopeId,
        createdAt = now,
        createdBy = "integration_test"
      ))
    }

    def createScope(name: String, parentId: Option[String]) = {
      scopeDao.insertScope(ScopeToInsert(
        name = name,
        parentId = parentId,
        description = None,
        isActive = 1,
        createdBy = "integration_test",
        createdAt = now
      ))
    }

    scopesList.foreach{ s ⇒
      (for {
        scopeParent ← createScope(s.parent, None)
        scopeParentIdSome = Some(scopeParent.id)
        scopeCreate ← createScope(s.create, scopeParentIdSome)
        scopeUpdate ← createScope(s.update, scopeParentIdSome)
        scopeDetail ← createScope(s.detail, scopeParentIdSome)
        scopeDelete ← createScope(s.delete, scopeParentIdSome)
        _ ← createPermission(scopeParent.id)
        _ ← createPermission(scopeCreate.id)
        _ ← createPermission(scopeUpdate.id)
        _ ← createPermission(scopeDetail.id)
        _ ← createPermission(scopeDelete.id)
      } yield {
        ()
      }).recover {
        case err: Exception⇒
          logger.error("Error creating scope from ScopesList", err)
      }
    }

  }






}
