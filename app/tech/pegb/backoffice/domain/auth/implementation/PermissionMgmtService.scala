package tech.pegb.backoffice.domain.auth.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.domain.auth.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import tech.pegb.backoffice.dao.auth.abstraction.PermissionDao
import tech.pegb.backoffice.dao.auth.entity
import tech.pegb.backoffice.domain.auth.abstraction.PermissionManagement
import tech.pegb.backoffice.domain.auth.dto.{PermissionCriteria, PermissionToCreate, PermissionToUpdate}
import tech.pegb.backoffice.domain.auth.model.Permission
import tech.pegb.backoffice.domain.{BaseService, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.auth.permission.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.permission.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

class PermissionMgmtService @Inject() (
    dao: PermissionDao,
    executionContexts: WithExecutionContexts)
  extends PermissionManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  def createPermission(createDto: PermissionToCreate, reactivate: Boolean): Future[ServiceResponse[Permission]] = {
    val (buId, roleId, userId) = createDto.permissionKey match {
      case BusinessUnitAndRolePermissionKey(b, r) ⇒ (b.some, r.some, none[UUID])
      case UserPermissionKey(u) ⇒ (none[UUID], none[UUID], u.some)
    }
    if (reactivate) {
      (for {
        getResultSeq ← EitherT.fromEither[Future](
          dao.getPermissionByCriteria(PermissionCriteria(
            businessId = buId.map(x ⇒ UUIDLike(x.toString)),
            roleId = roleId.map(x ⇒ UUIDLike(x.toString)),
            userId = userId.map(x ⇒ UUIDLike(x.toString))).asDao(false), None, None, None).asServiceResponse)
        resultOption ← getResultSeq.headOption match {
          case Some(permission) ⇒
            logger.debug("[createPermission] Reactivating via PermissionDao.updatePermission")
            EitherT.fromEither[Future](dao.updatePermission(permission.id, createDto.asReactivateDao(permission.updatedAt)).asServiceResponse)
          case None ⇒
            logger.debug(s"[createPermission] Permission to reactivate was not found. Creating a new permission based from dto: $createDto")
            EitherT.fromEither[Future](dao.insertPermission(createDto.asDao).asServiceResponse).map(_.some)
        }
        result ← EitherT.fromOption[Future](resultOption, notFoundError("Reactivated or Created permission not found"))
        domainResult ← EitherT.fromEither[Future](result.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert permission entity to domain: ${t.getMessage}")))
      } yield {
        domainResult
      }).value
    } else {
      (for {
        result ← EitherT.fromEither[Future](dao.insertPermission(createDto.asDao).asServiceResponse)
        domainResult ← EitherT.fromEither[Future](result.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert permission entity to domain: ${t.getMessage}")))
      } yield domainResult).value
    }
  }

  def getPermissionById(id: UUID): Future[ServiceResponse[Permission]] = {
    (for {
      getByCriteriaResult ← EitherT(getPermissionByCriteria(PermissionCriteria(id = UUIDLike(id.toString).some), Nil, None, None))
      getResult ← EitherT.fromOption[Future](getByCriteriaResult.headOption, notFoundError(s"Permission $id is not found"))
    } yield {
      getResult
    }).value
  }

  def getPermissionByCriteria(
    criteriaDto: PermissionCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Permission]]] = Future {
    dao.getPermissionByCriteria(criteriaDto.asDao(isActive = true), ordering.asDao, limit, offset)
      .map(_.flatMap(_.asDomain.toOption))
      .asServiceResponse
  }

  def countByCriteria(criteriaDto: PermissionCriteria): Future[ServiceResponse[Int]] = Future {
    dao.countPermissionByCriteria(criteriaDto.asDao()).asServiceResponse
  }

  def updatePermissionById(id: UUID, updateDto: PermissionToUpdate): Future[ServiceResponse[Permission]] = {

    (for {
      getResult ← EitherT(getPermissionById(id))
      updateResultOption ← EitherT.fromEither[Future]((updateDto.permissionKey, getResult.permissionKey) match {
        case (None, _) |
          (Some(BusinessUnitAndRolePermissionKey(_, _)), BusinessUnitAndRolePermissionKey(_, _)) |
          (Some(UserPermissionKey(_)), UserPermissionKey(_)) ⇒
          dao.updatePermission(id.toString, updateDto.asDao(isActive = true.some, getResult.updatedAt)).asServiceResponse
        case _ ⇒
          validationError(s"[updatePermissionById] Changing permission key type is not allowed. Permission $id has ${getResult.permissionKey.keyType}")
            .asLeft[Option[entity.Permission]]
      })
      updateResult ← EitherT.fromOption[Future](updateResultOption, notFoundError(s"Permission $id is not found"))
      domainResult ← EitherT.fromEither[Future](updateResult.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert permission entity to domain: ${t.getMessage}")))
    } yield {
      domainResult
    }).value
  }

  def deletePermissionById(id: UUID, updatedAt: LocalDateTime, updatedBy: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      maybeMissingLastUpdatedAt ← EitherT.fromEither[Future](dao.getPermissionByCriteria(
        PermissionCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError))

      updateResultOption ← EitherT.fromEither[Future](dao.updatePermission(
        id.toString,
        PermissionToUpdate(updatedAt = updatedAt, updatedBy = updatedBy, lastUpdatedAt = lastUpdatedAt)
          .asDao(isActive = false.some, maybeMissingLastUpdatedAt)).asServiceResponse)
      _ ← EitherT.fromOption[Future](updateResultOption, notFoundError(s"Permission $id is not found"))
    } yield {
      ()
    }).value
  }
}
