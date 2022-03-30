package tech.pegb.backoffice.domain.auth.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.auth.abstraction.{PermissionDao, ScopeDao}
import tech.pegb.backoffice.dao.auth.entity
import tech.pegb.backoffice.domain.auth.abstraction.ScopeManagement
import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.domain.auth.model.Scope
import tech.pegb.backoffice.domain.{BaseService, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.auth.scope.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.permission.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.scope.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

class ScopeMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    dao: ScopeDao,
    permissionDao: PermissionDao)
  extends ScopeManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  def createScope(createDto: ScopeToCreate, reactivate: Boolean): Future[ServiceResponse[Scope]] = {
    if (reactivate) {
      (for {
        getResultSeq ← EitherT.fromEither[Future](
          dao.getScopeByCriteria(ScopeCriteria(name = createDto.name.some).asDao(isActive = false), None, None, None).asServiceResponse)
        resultOption ← getResultSeq.headOption match {
          case Some(scope) ⇒
            logger.debug("[createScope] Reactivating via ScopeDao.updateScope")
            EitherT.fromEither[Future](dao.updateScope(scope.id, createDto.asReactivateDao(scope.updatedAt)).asServiceResponse)
          case None ⇒
            logger.debug(s"[createScope] Scope to reactivate was not found. Creating a new scope based from dto: $createDto")
            for {
              _ ← createDto.parentId match {
                case None ⇒ EitherT.fromEither[Future](().asRight[ServiceError])
                case Some(id) ⇒ EitherT(getScopeById(id)).map(_ ⇒ ()).leftMap(_ ⇒ validationError(s"Parent Scope with id $id doesn't exist"))
              }
              insertRes ← EitherT.fromEither[Future](dao.insertScope(createDto.asDao).asServiceResponse).map(_.some)
            } yield insertRes
        }
        result ← EitherT.fromOption[Future](resultOption, notFoundError("Reactivated or Created scope not found"))
        domainResult ← EitherT.fromEither[Future](result.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert scope entity to domain: ${t.getMessage}")))
      } yield {
        domainResult
      }).value
    } else {
      (for {
        _ ← createDto.parentId match {
          case None ⇒ EitherT.fromEither[Future](().asRight[ServiceError])
          case Some(id) ⇒ EitherT(getScopeById(id)).map(_ ⇒ ()).leftMap(_ ⇒ validationError(s"Parent Scope with id $id doesn't exist"))
        }
        result ← EitherT.fromEither[Future](dao.insertScope(createDto.asDao).asServiceResponse)
        domainResult ← EitherT.fromEither[Future](result.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert scope entityto domain: ${t.getMessage}")))
      } yield {
        domainResult
      }).value
    }
  }

  def getScopeById(id: UUID): Future[ServiceResponse[Scope]] = {
    (for {
      getByCriteriaResult ← EitherT(getScopeByCriteria(ScopeCriteria(id = UUIDLike(id.toString).some), Nil, None, None))
      getResult ← EitherT.fromOption[Future](getByCriteriaResult.headOption, notFoundError(s"Scope $id is not found"))
    } yield {
      getResult
    }).value
  }

  def getScopeByCriteria(criteriaDto: ScopeCriteria, ordering: Seq[model.Ordering], limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Scope]]] = Future {
    dao.getScopeByCriteria(criteriaDto.asDao(), ordering.asDao, limit, offset)
      .map(_.flatMap(_.asDomain.toOption))
      .asServiceResponse
  }

  def countByCriteria(criteriaDto: ScopeCriteria): Future[ServiceResponse[Int]] = Future {
    dao.countScopeByCriteria(criteriaDto.asDao()).asServiceResponse
  }

  def updateScopeById(id: UUID, updateDto: ScopeToUpdate): Future[ServiceResponse[Scope]] = {
    (for {
      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← EitherT.fromEither[Future](dao.getScopeByCriteria(
        ScopeCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError))

      updateResultOption ← EitherT.fromEither[Future](dao.updateScope(
        id.toString,
        updateDto.asDao(isActive = true.some, maybeMissingLastUpdatedAt)).asServiceResponse)
      updateResult ← EitherT.fromOption[Future](updateResultOption, notFoundError(s"Scope $id is not found"))
      domainResult ← EitherT.fromEither[Future](updateResult.asDomain.toEither.leftMap(t ⇒ dtoMappingError(s"Failed to convert scope entity to domain: ${t.getMessage}")))
    } yield {
      domainResult
    }).value
  }

  def deleteScopeById(id: UUID, updatedAt: LocalDateTime, updatedBy: String, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {
      permission ← EitherT.fromEither[Future](permissionDao.getPermissionByCriteria(
        PermissionCriteria(scopeId = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None).asServiceResponse)

      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← EitherT.fromEither[Future](dao.getScopeByCriteria(
        ScopeCriteria(id = UUIDLike(id.toString).some).asDao(isActive = true), None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError))

      updateResultOption ← EitherT.fromEither[Future] {
        if (permission.isEmpty) {
          dao.updateScope(
            id.toString,
            ScopeToUpdate(updatedAt = updatedAt, updatedBy = updatedBy,
              lastUpdatedAt = lastUpdatedAt).asDao(isActive = false.some, maybeMissingLastUpdatedAt)).asServiceResponse
        } else {
          validationError(s"Scope to delete has Permissions. Cannot delete scope $id.").asLeft[Option[entity.Scope]]
        }
      }
      _ ← EitherT.fromOption[Future](updateResultOption, notFoundError(s"Scope $id is not found"))
    } yield {
      ()
    }).value
  }

}
