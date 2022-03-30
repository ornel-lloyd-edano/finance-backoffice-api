package tech.pegb.backoffice.domain.auth

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.auth.abstraction.{BackOfficeUserDao, BusinessUnitDao, RoleDao}
import tech.pegb.backoffice.dao.auth.dto.{BackOfficeUserCriteria ⇒ DaoBackOfficeUserCriteria, BackOfficeUserToUpdate ⇒ DaoBackOfficeUserToUpdate}
import tech.pegb.backoffice.dao.auth.entity.{BackOfficeUser ⇒ DaoBackOfficeUser}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.domain.{EmailClient, ErrorCodes, ServiceError}
import tech.pegb.backoffice.domain.ServiceError.DTOMappingError
import tech.pegb.backoffice.domain.auth.abstraction.{BackOfficeUserService, PasswordService, PermissionManagement}
import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.role.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}
import tech.pegb.backoffice.util.Constants.UnitInstance
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class BackOfficeUserServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val passwordService = stub[PasswordService]
  val emailClient = stub[EmailClient]
  val roleDao = stub[RoleDao]
  val businessUnitDao = stub[BusinessUnitDao]
  val backOfficeUserDao = stub[BackOfficeUserDao]
  val permissionService = stub[PermissionManagement]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[PermissionManagement].to(permissionService),
      bind[BackOfficeUserDao].to(backOfficeUserDao),
      bind[PasswordService].to(passwordService),
      bind[EmailClient].to(emailClient),
      bind[RoleDao].to(roleDao),
      bind[BusinessUnitDao].to(businessUnitDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val backOfficeUserService = inject[BackOfficeUserService]

  "BackOfficeUserService" should {
    "count active backoffice users by criteria" in {
      val criteria = BackOfficeUserCriteria()

      (backOfficeUserDao.countBackOfficeUsersByCriteria _).when(criteria.asDao(true.toOption).toOption).returns(Right(10))

      val result = backOfficeUserService.countActiveBackOfficeUsersByCriteria(criteria.toOption)
      val expected = 10

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "get active backoffice users by criteria" in {
      val criteria = BackOfficeUserCriteria()

      def uuid = UUID.randomUUID().toString

      val mockResults = Seq(
        DaoBackOfficeUser.empty.copy(id = uuid, userName = "user1", firstName = "name1", lastName = "name1", email = "user@pegb.tech", createdBy = Some("user"), roleId = uuid, roleName = "role1", roleCreatedBy = Some("user"), businessUnitId = uuid, businessUnitName = "bu1", businessUnitCreatedBy = Some("user")),
        DaoBackOfficeUser.empty.copy(id = uuid, userName = "user2", firstName = "name2", lastName = "name3", email = "user@pegb.tech", createdBy = Some("user"), roleId = uuid, roleName = "role2", roleCreatedBy = Some("user"), businessUnitId = uuid, businessUnitName = "bu2", businessUnitCreatedBy = Some("user")),
        DaoBackOfficeUser.empty.copy(id = uuid, userName = "user3", firstName = "name3", lastName = "name3", email = "user@pegb.tech", createdBy = Some("user"), roleId = uuid, roleName = "role3", roleCreatedBy = Some("user"), businessUnitId = uuid, businessUnitName = "bu3", businessUnitCreatedBy = Some("user")))

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[tech.pegb.backoffice.dao.auth.dto.BackOfficeUserCriteria], _: Option[tech.pegb.backoffice.dao.model.OrderingSet], _: Option[Int], _: Option[Int]))
        .when(criteria.asDao(isActive = true.toOption).toOption, None, None, None)
        .returns(Right(mockResults))

      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(mockResults(0).businessUnitId).some,
            roleId = UUIDLike(mockResults(0).roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))
      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(mockResults(1).businessUnitId).some,
            roleId = UUIDLike(mockResults(1).roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))
      (permissionService.getPermissionByCriteria _)
        .when(
          PermissionCriteria(
            businessId = UUIDLike(mockResults(2).businessUnitId).some,
            roleId = UUIDLike(mockResults(2).roleId).some),
          Nil, None, None)
        .returns(Future.successful(Nil.asRight[ServiceError]))

      val result = backOfficeUserService.getActiveBackOfficeUsersByCriteria(
        criteria.toOption, Seq.empty, None, None)
      val expected = mockResults.map(_.asDomain.get)

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "fail to get active backoffice users by criteria if dao entity cannot be mapped to domain model" in {
      val criteria = BackOfficeUserCriteria()

      def uuid = UUID.randomUUID().toString

      val mockResults = Seq(
        DaoBackOfficeUser.empty.copy(id = uuid, userName = "", firstName = "name1", lastName = "name1", email = "user@pegb.tech", createdBy = Some("user"), roleId = uuid, roleName = "role1", roleCreatedBy = Some("user"), businessUnitId = uuid, businessUnitName = "bu1", businessUnitCreatedBy = Some("user")),
        DaoBackOfficeUser.empty.copy(id = uuid, userName = "", firstName = "name2", lastName = "name3", email = "user@pegb.tech", createdBy = Some("user"), roleId = uuid, roleName = "role2", roleCreatedBy = Some("user"), businessUnitId = uuid, businessUnitName = "bu2", businessUnitCreatedBy = Some("user")),
        DaoBackOfficeUser.empty.copy(id = uuid, userName = "", firstName = "name3", lastName = "name3", email = "user@pegb.tech", createdBy = Some("user"), roleId = uuid, roleName = "role3", roleCreatedBy = Some("user"), businessUnitId = uuid, businessUnitName = "bu3", businessUnitCreatedBy = Some("user")))

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[tech.pegb.backoffice.dao.auth.dto.BackOfficeUserCriteria], _: Option[tech.pegb.backoffice.dao.model.OrderingSet], _: Option[Int], _: Option[Int]))
        .when(criteria.asDao(isActive = true.toOption).toOption, None, None, None)
        .returns(Right(mockResults))

      val result = backOfficeUserService.getActiveBackOfficeUsersByCriteria(
        criteria.toOption, Seq.empty, None, None)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.isInstanceOf[DTOMappingError] mustBe true
      }
    }

    "create backoffice user" in {
      val dto = BackOfficeUserToCreate.empty.copy(userName = "user1", email = Email("user@pegb.tech"),
        phoneNumber = Some("0544451678"), firstName = "lloyd", lastName = "edano",
        roleId = UUID.randomUUID(), businessUnitId = UUID.randomUUID(), createdBy = "admin", createdAt = LocalDateTime.now)

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[DaoBackOfficeUserCriteria], _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(userName = Some(dto.userName))
          .asDao(isActive = Some(false)).toOption, None, None, None).returns(Right(Seq.empty))

      val mockGeneratedPassword = "random password"
      (passwordService.generatePassword _).when().returns(mockGeneratedPassword)

      val hashedPassword = "hashed password"
      (passwordService.hashPassword _).when(mockGeneratedPassword).returns(hashedPassword)

      val mockInput = dto.asDao(hashedPassword)

      val mockResult = DaoBackOfficeUser.empty.copy(
        id = UUID.randomUUID().toString,
        userName = dto.userName, password = Option(hashedPassword), email = dto.email.value,
        phoneNumber = dto.phoneNumber, firstName = dto.firstName, lastName = dto.lastName,
        roleId = dto.roleId.toString, roleName = "role1", roleLevel = 1, roleCreatedBy = Some("admin"),
        businessUnitId = dto.businessUnitId.toString, businessUnitName = "bu1", businessUnitCreatedBy = Some("admin"),
        createdBy = Some(dto.createdBy), createdAt = Some(dto.createdAt))

      (backOfficeUserDao.createBackOfficeUser _).when(mockInput).returns(Right(mockResult))

      (emailClient.sendEmail(_: Seq[String], _: String, _: String)).when(
        Seq(dto.email.value),
        "Backoffice credentials",
        s"""Hello ${dto.firstName + " " + dto.lastName},
           |
           |Your registration was successful.
           |Username: ${dto.userName}
           |Password: $mockGeneratedPassword
           |
           |Kind regards,
           |BackOffice API""".stripMargin).returns(Right(UnitInstance))

      val result = backOfficeUserService.createBackOfficeUser(dto, false)

      val expected = mockResult.asDomain.get

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "recreate a deactivated backoffice user" in {
      val dto = BackOfficeUserToCreate.empty.copy(userName = "user1", email = Email("user@pegb.tech"),
        phoneNumber = Some("0544451678"), firstName = "lloyd", lastName = "edano",
        roleId = UUID.randomUUID(), businessUnitId = UUID.randomUUID(), createdBy = "admin", createdAt = LocalDateTime.now)

      val mockExisting = DaoBackOfficeUser.empty.copy(
        id = UUID.randomUUID().toString,
        userName = dto.userName, password = Option("password"), email = dto.email.value,
        phoneNumber = dto.phoneNumber, firstName = dto.firstName, lastName = dto.lastName,
        roleId = dto.roleId.toString, roleName = "role1", roleLevel = 1, roleCreatedBy = Some("admin"),
        businessUnitId = dto.businessUnitId.toString, businessUnitName = "bu1", businessUnitCreatedBy = Some("admin"),
        createdBy = Some(dto.createdBy), createdAt = Some(dto.createdAt), isActive = 0)

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[DaoBackOfficeUserCriteria], _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(userName = Some(dto.userName))
          .asDao(isActive = Some(false)).toOption, None, None, None).returns(Right(Seq(mockExisting)))

      val mockGeneratedPassword = "random password"
      (passwordService.generatePassword _).when().returns(mockGeneratedPassword)

      val hashedPassword = "hashed password"
      (passwordService.hashPassword _).when(mockGeneratedPassword).returns(hashedPassword)

      val mockInput = dto.asDao(hashedPassword)

      val mockUpdateInput = BackOfficeUserToUpdate(
        updatedBy = dto.createdBy,
        updatedAt = dto.createdAt,
        lastUpdatedAt = None).asDao(isActive = true.toOption, maybeNewPassword = hashedPassword.toOption)

      val mockUpdateResult = DaoBackOfficeUser.empty.copy(
        id = UUID.randomUUID().toString,
        userName = dto.userName, password = Option(hashedPassword), email = dto.email.value,
        phoneNumber = dto.phoneNumber, firstName = dto.firstName, lastName = dto.lastName,
        roleId = dto.roleId.toString, roleName = "role1", roleLevel = 1, roleCreatedBy = Some("admin"),
        businessUnitId = dto.businessUnitId.toString, businessUnitName = "bu1", businessUnitCreatedBy = Some("admin"),
        createdBy = Some(dto.createdBy), createdAt = Some(dto.createdAt),
        updatedBy = Some(mockUpdateInput.updatedBy), updatedAt = Some(mockUpdateInput.updatedAt))

      (backOfficeUserDao.updateBackOfficeUser(_: String, _: DaoBackOfficeUserToUpdate))
        .when(mockExisting.id, mockUpdateInput).returns(Right(mockUpdateResult.toOption))

      (emailClient.sendEmail(_: Seq[String], _: String, _: String)).when(
        Seq(dto.email.value),
        "Backoffice credentials",
        s"""Hello ${dto.firstName + " " + dto.lastName},
           |
           |Your registration was successful.
           |Username: ${dto.userName}
           |Password: $mockGeneratedPassword
           |
           |Kind regards,
           |BackOffice API""".stripMargin).returns(Right(UnitInstance))

      val result = backOfficeUserService.createBackOfficeUser(dto, reactivateIfExisting = true)

      val expected = mockUpdateResult.asDomain.get

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "fail to recreate a deactivated backoffice user if reactivate flag is set to false" in {
      val dto = BackOfficeUserToCreate.empty.copy(userName = "user1", email = Email("user@pegb.tech"),
        phoneNumber = Some("0544451678"), firstName = "lloyd", lastName = "edano",
        roleId = UUID.randomUUID(), businessUnitId = UUID.randomUUID(), createdBy = "admin", createdAt = LocalDateTime.now)

      val mockExisting = DaoBackOfficeUser.empty.copy(
        id = UUID.randomUUID().toString,
        userName = dto.userName, password = Option("password"), email = dto.email.value,
        phoneNumber = dto.phoneNumber, firstName = dto.firstName, lastName = dto.lastName,
        roleId = dto.roleId.toString, roleName = "role1", roleLevel = 1, roleCreatedBy = Some("admin"),
        businessUnitId = dto.businessUnitId.toString, businessUnitName = "bu1", businessUnitCreatedBy = Some("admin"),
        createdBy = Some(dto.createdBy), createdAt = Some(dto.createdAt), isActive = 0)

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[DaoBackOfficeUserCriteria], _: Option[OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(userName = Some(dto.userName))
          .asDao(isActive = Some(false)).toOption, None, None, None).returns(Right(Seq(mockExisting)))

      val mockGeneratedPassword = "random password"
      (passwordService.generatePassword _).when().returns(mockGeneratedPassword)

      val result = backOfficeUserService.createBackOfficeUser(dto, reactivateIfExisting = false)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.Duplicate
      }
    }

    "update a backoffice user by id" in {
      val buIdToUpdate = UUID.randomUUID()
      val dto = BackOfficeUserToUpdate.empty.copy(
        email = Some(Email("new_email@pegb.tech")),
        phoneNumber = Some("+971512351987"), updatedBy = "PegbUser")

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(phoneNumber = Some("+971512351987")).asDao(isActive = None, butNotThisId = buIdToUpdate.toOption).toOption)
        .returns(Right(0))

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(email = Some(Email("new_email@pegb.tech"))).asDao(isActive = None, butNotThisId = buIdToUpdate.toOption).toOption)
        .returns(Right(0))

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[tech.pegb.backoffice.dao.auth.dto.BackOfficeUserCriteria], _: Option[tech.pegb.backoffice.dao.model.OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(id = Some(UUIDLike(buIdToUpdate.toString))).asDao(isActive = true.toOption).toOption, None, None, None)
        .returns(Right(Seq(DaoBackOfficeUser.empty.copy(updatedAt = None))))

      val mockResult = DaoBackOfficeUser.empty.copy(
        id = buIdToUpdate.toString,
        userName = "lloyd", password = Option("password"), email = dto.email.map(_.value).get,
        phoneNumber = dto.phoneNumber, firstName = "Lloyd", lastName = "Edano",
        roleId = UUID.randomUUID().toString, roleName = "role1", roleLevel = 1, roleCreatedBy = Some("admin"),
        businessUnitId = UUID.randomUUID().toString, businessUnitName = "bu1", businessUnitCreatedBy = Some("admin"),
        updatedBy = dto.updatedBy.toOption, updatedAt = dto.updatedAt.toOption)

      (backOfficeUserDao.updateBackOfficeUser(_: String, _: DaoBackOfficeUserToUpdate))
        .when(buIdToUpdate.toString, dto.asDao(isActive = Some(true))).returns(Right(mockResult.toOption))

      val result = backOfficeUserService.updateBackOfficeUser(buIdToUpdate, dto)

      val expected = mockResult.asDomain.get

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "fail to update a backoffice user by id if not found" in {
      val buIdToUpdate = UUID.randomUUID()
      val dto = BackOfficeUserToUpdate.empty.copy(
        email = Some(Email("new_email@pegb.tech")),
        phoneNumber = Some("+971512351987"), updatedBy = "PegbUser")

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(phoneNumber = Some("+971512351987")).asDao(isActive = None, butNotThisId = buIdToUpdate.toOption).toOption)
        .returns(Right(0))

      (backOfficeUserDao.countBackOfficeUsersByCriteria _)
        .when(BackOfficeUserCriteria(email = Some(Email("new_email@pegb.tech"))).asDao(isActive = None, butNotThisId = buIdToUpdate.toOption).toOption)
        .returns(Right(0))

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[tech.pegb.backoffice.dao.auth.dto.BackOfficeUserCriteria], _: Option[tech.pegb.backoffice.dao.model.OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(id = Some(UUIDLike(buIdToUpdate.toString))).asDao(isActive = true.toOption).toOption, None, None, None)
        .returns(Right(Seq(DaoBackOfficeUser.empty.copy(updatedAt = None))))

      (backOfficeUserDao.updateBackOfficeUser(_: String, _: DaoBackOfficeUserToUpdate))
        .when(buIdToUpdate.toString, dto.asDao(isActive = Some(true))).returns(Right(None))

      val result = backOfficeUserService.updateBackOfficeUser(buIdToUpdate, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.NotFound
      }
    }

    "fail to update roleId of a backoffice user if role id not found" in {
      val buIdToUpdate = UUID.randomUUID()
      val dto = BackOfficeUserToUpdate.empty.copy(
        roleId = Some(UUID.randomUUID()), updatedBy = "PegbUser")

      (roleDao.countRolesByCriteria _)
        .when(RoleCriteria(id = dto.roleId).asDao(isActive = Some(true)).toOption)
        .returns(Right(0))

      val result = backOfficeUserService.updateBackOfficeUser(buIdToUpdate, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.NotFound
      }
    }

    "fail to update businessUnitId of a backoffice user if business unit id not found" in {
      val buIdToUpdate = UUID.randomUUID()
      val dto = BackOfficeUserToUpdate.empty.copy(
        businessUnitId = Some(UUID.randomUUID()), updatedBy = "PegbUser")

      (businessUnitDao.countBusinessUnitsByCriteria _)
        .when(BusinessUnitCriteria(id = dto.businessUnitId).asDao(isActive = true))
        .returns(Right(0))

      val result = backOfficeUserService.updateBackOfficeUser(buIdToUpdate, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.NotFound
      }
    }

    "fail to update a backoffice user by id if email is already taken (whether by active or inactive user)" in {
      val buIdToUpdate = UUID.randomUUID()

      val dto = BackOfficeUserToUpdate(email = Some(Email("old_email@pegb.tech")), updatedBy = "admin", updatedAt = LocalDateTime.now)

      val mockInput = BackOfficeUserCriteria(email = dto.email).asDao(isActive = None, butNotThisId = buIdToUpdate.toOption)

      (backOfficeUserDao.countBackOfficeUsersByCriteria _).when(mockInput.toOption).returns(Right(1))

      val result = backOfficeUserService.updateBackOfficeUser(buIdToUpdate, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.ValidationFailed
      }
    }

    "fail to update a backoffice user by id if phone number is already taken (whether by active or inactive user)" in {
      val buIdToUpdate = UUID.randomUUID()
      val dto = BackOfficeUserToUpdate(phoneNumber = Some("0544451666"), updatedBy = "admin", updatedAt = LocalDateTime.now)

      val mockInput = BackOfficeUserCriteria(phoneNumber = dto.phoneNumber).asDao(isActive = None, butNotThisId = buIdToUpdate.toOption)
      (backOfficeUserDao.countBackOfficeUsersByCriteria _).when(mockInput.toOption).returns(Right(1))

      val result = backOfficeUserService.updateBackOfficeUser(buIdToUpdate, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.ValidationFailed
      }
    }

    "deactivate a backoffice user by id" in {
      val buIdToDeactivate = UUID.randomUUID()

      val dto = BackOfficeUserToRemove(removedBy = "admin", removedAt = LocalDateTime.now, lastUpdatedAt = None)

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[tech.pegb.backoffice.dao.auth.dto.BackOfficeUserCriteria], _: Option[tech.pegb.backoffice.dao.model.OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(id = Some(UUIDLike(buIdToDeactivate.toString))).asDao(isActive = true.toOption).toOption, None, None, None)
        .returns(Right(Seq(DaoBackOfficeUser.empty.copy(updatedAt = None))))

      (backOfficeUserDao.updateBackOfficeUser(_: String, _: DaoBackOfficeUserToUpdate))
        .when(
          buIdToDeactivate.toString,
          BackOfficeUserToUpdate.empty.copy(
            updatedAt = dto.removedAt, updatedBy = dto.removedBy,
            lastUpdatedAt = dto.lastUpdatedAt).asDao(isActive = Some(false)))
        .returns(Right(DaoBackOfficeUser.empty.toOption))

      val result = backOfficeUserService.removeBackOfficeUser(buIdToDeactivate, dto)

      val expected = UnitInstance

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "considers successful remove of backoffice user even if not found" in {
      val buIdToDeactivate = UUID.randomUUID()

      val dto = BackOfficeUserToRemove(removedBy = "admin", removedAt = LocalDateTime.now, lastUpdatedAt = None)

      (backOfficeUserDao.getBackOfficeUsersByCriteria(_: Option[tech.pegb.backoffice.dao.auth.dto.BackOfficeUserCriteria], _: Option[tech.pegb.backoffice.dao.model.OrderingSet], _: Option[Int], _: Option[Int]))
        .when(BackOfficeUserCriteria(id = Some(UUIDLike(buIdToDeactivate.toString))).asDao(isActive = true.toOption).toOption, None, None, None)
        .returns(Right(Seq(DaoBackOfficeUser.empty.copy(updatedAt = None))))

      (backOfficeUserDao.updateBackOfficeUser(_: String, _: DaoBackOfficeUserToUpdate))
        .when(
          buIdToDeactivate.toString,
          BackOfficeUserToUpdate.empty.copy(
            updatedAt = dto.removedAt, updatedBy = dto.removedBy,
            lastUpdatedAt = dto.lastUpdatedAt).asDao(isActive = Some(false)))
        .returns(Right(None))

      val result = backOfficeUserService.removeBackOfficeUser(buIdToDeactivate, dto)

      val expected = UnitInstance

      whenReady(result) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expected
      }
    }

    "fail to deactivate a backoffice user by id dao error" in {
      val buIdToDeactivate = UUID.randomUUID()

      val dto = BackOfficeUserToRemove(removedBy = "admin", removedAt = LocalDateTime.now, lastUpdatedAt = None)

      (backOfficeUserDao.updateBackOfficeUser(_: String, _: DaoBackOfficeUserToUpdate))
        .when(
          buIdToDeactivate.toString,
          BackOfficeUserToUpdate.empty.copy(
            updatedAt = dto.removedAt, updatedBy = dto.removedBy,
            lastUpdatedAt = dto.lastUpdatedAt).asDao(isActive = Some(false)))
        .returns(Left(DaoError.GenericDbError("some error")))

      val result = backOfficeUserService.removeBackOfficeUser(buIdToDeactivate, dto)

      whenReady(result) { result ⇒
        result.isLeft mustBe true
        result.left.get.code mustBe ErrorCodes.Unknown
      }
    }
  }
}
