package tech.pegb.backoffice.domain.auth.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.auth.abstraction.RoleDao
import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.domain.auth.model.Role
import tech.pegb.backoffice.domain.auth.{dto, _}
import tech.pegb.backoffice.domain.{ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.auth.role.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.role.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoleService @Inject() (
    executionContext: WithExecutionContexts,
    backOfficeUserService: abstraction.BackOfficeUserService,
    roleDao: RoleDao) extends abstraction.RoleService {

  implicit val ec: ExecutionContext = executionContext.blockingIoOperations
  private val active = true
  private val inactive = false

  def countActiveRolesByCriteria(criteria: Option[RoleCriteria]): Future[ServiceResponse[Int]] = Future {
    roleDao.countRolesByCriteria(criteria.map(_.asDao(isActive = Some(active)))).asServiceResponse
  }

  def getActiveRolesByCriteria(
    criteria: Option[RoleCriteria],
    orderBy: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Role]]] = Future {
    roleDao.getRolesByCriteria(criteria.map(_.asDao(isActive = active.some)), orderBy.asDao, limit, offset)
      .bimap(_.asDomainError, _.map(_.asDomain.get))
  }.recover {
    case error: IllegalArgumentException if error.getMessage.toLowerCase.contains("invalid uuid") ⇒
      logger.error("Error in getActiveRolesByCriteria. Id from role table is not UUID", error)
      dtoMappingError("Unable to read role resource correctly. Id was not UUID").toLeft

    case error: Exception if error.getCause.isInstanceOf[AssertionError] ⇒
      logger.error("Error in getActiveRolesByCriteria. Name or createdBy cannot be empty.", error)
      dtoMappingError("Unable to read role resource correctly. Name or created_by cannot be empty.").toLeft

    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def createActiveRole(dto: RoleToCreate, reactivateIfExisting: Boolean): Future[ServiceResponse[Role]] = Future {
    val criteria = RoleCriteria(name = dto.name.some)

    for {
      optionalRole ← roleDao.getRolesByCriteria(criteria.asDao().some, none, none, none).asServiceResponse.map(_.headOption)
      maybeRoleTry ← optionalRole match {
        case Some(role) if !reactivateIfExisting ⇒
          ServiceError.duplicateError(s"role with name ${role.name} already exists").toLeft

        case _ if !Role.isValidLevel(dto.level) ⇒
          ServiceError.validationError(s"role level must be between ${Role.minRoleLevel} to ${Role.maxRoleLevel} inclusive").toLeft

        case _ if dto.createdBy.isEmpty || !Role.isValidName(dto.name) ⇒
          ServiceError.validationError(s"name and created by can not be empty").toLeft

        case Some(role) if reactivateIfExisting ⇒
          val roleToUpdate = RoleToUpdate(updatedBy = dto.createdBy, updatedAt = dto.createdAt)

          roleDao.updateRole(role.id, roleToUpdate.asDao(isActive = active.some)).map(_.map(_.asDomain)).asServiceResponse

        case _ ⇒ roleDao.createRole(dto.asDao).map(_.asDomain.some).asServiceResponse
      }

      role ← maybeRoleTry.fold[ServiceResponse[Role]](ServiceError.notFoundError(s"no role found to activate with name ${dto.name}").toLeft) { domainRole ⇒ domainRole.toEither.leftMap(_ ⇒ ServiceError.dtoMappingError("error mapping dao role to domain")) }
    } yield role

  }

  def updateRole(id: UUID, dto: RoleToUpdate): Future[ServiceResponse[Role]] = Future {
    for {
      optionalRole ← roleDao.getRolesByCriteria(RoleCriteria(name = dto.name)
        .asDao(isActive = active.some, butNotThisId = id.some).some, none, none, none)
        .map(_.headOption).asServiceResponse

      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← roleDao.getRolesByCriteria(
        RoleCriteria(id = id.some).asDao(isActive = active.some).some, None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError)

      updatedRoleOptional ← (optionalRole, dto.level) match {
        case (Some(role), _) if dto.name.isDefined && role.name.equalsIgnoreCase(dto.name.getOrElse("")) ⇒
          ServiceError.duplicateError(s"role with name ${role.name} already exists").toLeft

        case (_, Some(level)) if !Role.isValidLevel(level) ⇒
          ServiceError.validationError(s"role level must be between ${Role.minRoleLevel} to ${Role.maxRoleLevel} inclusive").toLeft

        case _ if dto.updatedBy.isEmpty ⇒ ServiceError.validationError(s"updated by can not be empty").toLeft

        case _ if dto.name.isDefined && !Role.isValidName(dto.name.getOrElse("")) ⇒
          ServiceError.validationError(s"name can not be empty").toLeft

        case _ ⇒ roleDao.updateRole(id, dto.asDao(isActive = active.some, maybeMissingLastUpdatedAt)).map(_.map(_.asDomain)).asServiceResponse
      }
      updatedRole ← updatedRoleOptional
        .fold[ServiceResponse[Role]](ServiceError.notFoundError(s"no role with id $id found to update").toLeft)(_.toEither.leftMap(_ ⇒ ServiceError.dtoMappingError("error mapping dao role to domain")))
    } yield updatedRole

  }

  def removeRole(id: UUID, updatedBy: String, updatedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    val criteria = dto.BackOfficeUserCriteria(roleId = UUIDLike(id.toString).some)

    (for {
      optionalBackOfficeUser ← EitherT(backOfficeUserService.getActiveBackOfficeUsersByCriteria(criteria.some, Seq.empty, none, none)
        .map(_.map(_.headOption)))

      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← EitherT.fromEither[Future](roleDao.getRolesByCriteria(
        RoleCriteria(id = id.some).asDao(isActive = active.some).some, None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError))

      deactivatedRoleOptional ← EitherT.fromEither[Future] {
        optionalBackOfficeUser match {
          case Some(user) ⇒ ServiceError.unknownError(s"role can not be deleted, being used by back office user ${user.id}").toLeft
          case None ⇒
            val roleToUpdate = RoleToUpdate(updatedBy = updatedBy, updatedAt = updatedAt, lastUpdatedAt = lastUpdatedAt)

            roleDao.updateRole(id, roleToUpdate.asDao(isActive = inactive.some, maybeMissingLastUpdatedAt)).asServiceResponse
        }
      }
      _ ← EitherT.fromOption[Future](deactivatedRoleOptional, ServiceError.notFoundError("no role found to deactivate"))

    } yield ()).value
  }
}
