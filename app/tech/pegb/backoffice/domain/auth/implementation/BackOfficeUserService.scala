package tech.pegb.backoffice.domain.auth.implementation

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.auth.abstraction.{BackOfficeUserDao, BusinessUnitDao, RoleDao}
import tech.pegb.backoffice.domain.EmailClient
import tech.pegb.backoffice.domain.auth.abstraction.{PasswordService, PermissionManagement, BackOfficeUserService ⇒ BackOfficeUserServiceTrait}
import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.domain.auth.model.BackOfficeUser
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.DomainModelMappingException
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.role.Implicits._
import tech.pegb.backoffice.util.Constants.UnitInstance
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class BackOfficeUserService @Inject() (
    executionContexts: WithExecutionContexts,
    emailClient: EmailClient,
    passwordService: PasswordService,
    permissionMgmtService: PermissionManagement,
    roleDao: RoleDao,
    businessUnitDao: BusinessUnitDao,
    backOfficeUserDao: BackOfficeUserDao) extends BackOfficeUserServiceTrait {

  implicit val ec = executionContexts.blockingIoOperations

  def countActiveBackOfficeUsersByCriteria(criteria: Option[BackOfficeUserCriteria]): Future[ServiceResponse[Int]] = Future {
    backOfficeUserDao.countBackOfficeUsersByCriteria(criteria.map(_.asDao(isActive = Some(true)))).asServiceResponse
  }.recover {
    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def getActiveBackOfficeUsersByCriteria(
    criteria: Option[BackOfficeUserCriteria],
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[BackOfficeUser]]] = {

    (for {
      backOfficeUsers ← EitherT(Future(backOfficeUserDao.getBackOfficeUsersByCriteria(
        criteria.map(_.asDao(isActive = Some(true))),
        orderBy.asDao, limit, offset)
        .asServiceResponse(_.map(result ⇒ result.asDomain match {
          case Success(result) ⇒ result
          case Failure(error) ⇒ throw new DomainModelMappingException(result, "Unable to map back_office_user entity to domain model", error)
        }))).recover {
        case error: DomainModelMappingException ⇒
          logger.error("Error in getActiveBackOfficeUsersByCriteria", error)
          dtoMappingError("Unable to read back_office_users resource correctly.").toLeft
        case error: Throwable ⇒
          logger.error("Unexpected error", error)
          unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
      })
      backOfficeWithPermissions ← EitherT {
        Future.sequence(backOfficeUsers.map(
          b ⇒ EitherT(permissionMgmtService.getPermissionByCriteria(
            PermissionCriteria(
              businessId = UUIDLike(b.businessUnit.id.toString).some,
              roleId = UUIDLike(b.role.id.toString).some),
            Nil, None, None)).map(permissionSeq ⇒ b.copy(permissions = permissionSeq)).value).toList).map(_.sequence[ServiceResponse, BackOfficeUser])
      }
    } yield {
      backOfficeWithPermissions
    }).value

  }

  def createBackOfficeUser(dto: BackOfficeUserToCreate, reactivateIfExisting: Boolean): Future[ServiceResponse[BackOfficeUser]] = Future {
    //Note: create a default random password (according to password rules) for the new backoffice user and email that password

    val existingNameButInactiveCriteria = BackOfficeUserCriteria(userName = Some(dto.userName)).asDao(isActive = false.toOption)
    for {
      validatedCreateDto ← (
        Seq(BackOfficeUser.isValidName(dto.userName), BackOfficeUser.isValidName(dto.firstName), BackOfficeUser.isValidName(dto.lastName)).contains(false),
        dto.phoneNumber.map(BackOfficeUser.isValidPhoneNumber(_))) match {
          case (true, _) ⇒
            validationError("Create back_office_users failed. User_name, first_name and last_name cannot be empty.").toLeft
          case (_, Some(false)) ⇒
            validationError("Create back_office_users failed. Invalid phone_number.").toLeft
          case _ ⇒ dto.toRight
        }

      //as per PR discussion, we should not rely in db constraints for business rules
      //TODO add domain validation for existing userName or email or phone

      existingButInactive ← backOfficeUserDao.getBackOfficeUsersByCriteria(existingNameButInactiveCriteria.toOption, None, None, None)
        .map(_.headOption.map(_.asDomain.get)).leftMap(_.asDomainError)

      generatedPassword ← passwordService.generatePassword.toRight

      updateOrCreateResult ← if (existingButInactive.isDefined && reactivateIfExisting) {
        val updateDto = BackOfficeUserToUpdate(
          updatedBy = validatedCreateDto.createdBy,
          updatedAt = validatedCreateDto.createdAt,
          lastUpdatedAt = None).asDao(isActive = true.toOption, maybeNewPassword = passwordService.hashPassword(generatedPassword).toOption)

        backOfficeUserDao.updateBackOfficeUser(existingButInactive.get.id.toString, updateDto)
          .map(_.headOption.map(_.asDomain.get)).leftMap(_.asDomainError).fold(
            _.toLeft,
            {
              case Some(bu) ⇒ bu.toRight
              case None ⇒ notFoundError("Create back_office_users failed. Inactive back_office_users was not found.").toLeft
            })

      } else if (existingButInactive.isDefined && !reactivateIfExisting) {
        duplicateError("Create back_office_users failed. Recreate flag must be set to true.").toLeft
      } else {
        backOfficeUserDao.createBackOfficeUser(validatedCreateDto.asDao(passwordService.hashPassword(generatedPassword))).map(_.asDomain.get).leftMap(_.asDomainError)
      }

    } yield {
      emailPassword(dto, generatedPassword)

      updateOrCreateResult
    }
  }.recover {
    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  //TODO refactor this to get email template from either config or i18n api
  private def emailPassword(dto: BackOfficeUserToCreate, generatedPassword: String): Unit = Future {
    val topic = "Backoffice credentials"
    val recipientName = s"${dto.firstName} ${dto.lastName}"
    val message =
      s"""Hello $recipientName,
         |
         |Your registration was successful.
         |Username: ${dto.userName}
         |Password: $generatedPassword
         |
         |Kind regards,
         |BackOffice API""".stripMargin

    Try(emailClient.sendEmail(Seq(dto.email.value), topic, message)).fold(
      f ⇒
        {
          logger.debug(s"failed to send email for password to backoffice user, reason: ${f.getMessage}")
          ()
        }, _ ⇒ ())
  }

  def updateBackOfficeUser(id: UUID, dto: BackOfficeUserToUpdate): Future[ServiceResponse[BackOfficeUser]] = Future {

    for {
      email ← dto.email.fold[ServiceResponse[Unit]](UnitInstance.toRight) { _ ⇒
        backOfficeUserDao.countBackOfficeUsersByCriteria(
          BackOfficeUserCriteria(email = dto.email).asDao(butNotThisId = id.some).toOption) //search for both active and inactive
          .fold(
            _.asDomainError.toLeft,
            {
              case found if found > 0 ⇒
                validationError(s"Update back_office_users failed. Email is already used by someone else.").toLeft
              case _ ⇒
                UnitInstance.toRight
            })
      }

      phoneNum ← dto.phoneNumber.fold[ServiceResponse[Unit]](UnitInstance.toRight) { _ ⇒
        backOfficeUserDao.countBackOfficeUsersByCriteria(
          BackOfficeUserCriteria(phoneNumber = dto.phoneNumber).asDao(butNotThisId = id.some).toOption) //search for both active and inactive
          .fold(
            _.asDomainError.toLeft,
            {
              case found if found > 0 ⇒
                validationError(s"Update back_office_users failed. Phone number is already used by someone else.").toLeft
              case _ ⇒
                UnitInstance.toRight
            })
      }

      role ← dto.roleId.fold[ServiceResponse[Unit]](UnitInstance.toRight) { _ ⇒
        roleDao.countRolesByCriteria(
          RoleCriteria(id = dto.roleId).asDao(isActive = Some(true)).toOption)
          .fold(
            _.asDomainError.toLeft,
            {
              case 0 ⇒
                notFoundError(s"Update back_office_users failed. Role id not found.").toLeft
              case _ ⇒
                UnitInstance.toRight
            })
      }

      bu ← dto.businessUnitId.fold[ServiceResponse[Unit]](UnitInstance.toRight) { _ ⇒
        businessUnitDao.countBusinessUnitsByCriteria(
          BusinessUnitCriteria(id = dto.businessUnitId).asDao(isActive = true))
          .fold(
            _.asDomainError.toLeft,
            {
              case 0 ⇒ notFoundError(s"Update back_office_users failed. Business unit id not found.").toLeft
              case _ ⇒ Right(UnitInstance)
            })
      }

      validDto ← (dto.phoneNumber.map(BackOfficeUser.isValidPhoneNumber(_)), dto.updatedBy.hasSomething) match {
        case (Some(false), _) ⇒
          validationError("Update back_office_users failed. Phone number is in wrong format.").toLeft
        case (_, false) ⇒
          validationError("Update back_office_users failed. User doing the update is empty.").toLeft
        case _ ⇒
          dto.toRight
      }

      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← backOfficeUserDao.getBackOfficeUsersByCriteria(
        BackOfficeUserCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true.some).some, None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError)

      result ← {
        backOfficeUserDao.updateBackOfficeUser(
          id.toString,
          validDto.asDao(isActive = true.toOption, maybeMissingLastUpdatedAt = maybeMissingLastUpdatedAt))
          .fold(
            _.asDomainError.toLeft,
            {
              case Some(result) ⇒
                result.asDomain.fold(
                  _ ⇒ dtoMappingError(s"Update may have succeeded but unable to read back_office_users resource correctly.").toLeft,
                  _.toRight)
              case None ⇒
                notFoundError(s"Update back_office_users failed. Id [$id] not found.").toLeft
            })

      }

    } yield {
      result
    }
  }.recover {
    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def getBackOfficeUserByUsername(username: String): Future[ServiceResponse[BackOfficeUser]] = {
    (for {
      getByCriteriaResult ← EitherT(getActiveBackOfficeUsersByCriteria(BackOfficeUserCriteria(userName = username.some).some, Nil, None, None))
      getResult ← EitherT.fromOption[Future](getByCriteriaResult.headOption, notFoundError(s"BackOfficeUser with username '$username' was not found"))
      permissions ← EitherT(permissionMgmtService.getPermissionByCriteria(
        PermissionCriteria(
          businessId = UUIDLike(getResult.businessUnit.id.toString).some,
          roleId = UUIDLike(getResult.role.id.toString).some),
        Nil, None, None))
    } yield {
      getResult.copy(permissions = permissions)
    }).value
  }

  def removeBackOfficeUser(id: UUID, dto: BackOfficeUserToRemove): Future[ServiceResponse[Unit]] = Future {
    for {
      validDto ← dto.removedBy.hasSomething
        .toEither(validationError("Remove back_office_user failed. User cannot be empty."), dto)

      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← backOfficeUserDao.getBackOfficeUsersByCriteria(
        BackOfficeUserCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true.some).some, None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError)

      result ← {
        val updateDto = BackOfficeUserToUpdate.empty.copy(updatedAt = validDto.removedAt, updatedBy = validDto.removedBy, lastUpdatedAt = dto.lastUpdatedAt)
          .asDao(isActive = Some(false), maybeMissingLastUpdatedAt = maybeMissingLastUpdatedAt)
        backOfficeUserDao.updateBackOfficeUser(id.toString, updateDto)
          .fold(
            _.asDomainError.toLeft,
            _ ⇒ Right(UnitInstance)) //if update did not found then consider remove a success
      }

    } yield {
      result
    }
  }.recover {
    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

}
