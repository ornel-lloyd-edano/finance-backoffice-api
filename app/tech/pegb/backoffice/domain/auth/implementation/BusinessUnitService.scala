package tech.pegb.backoffice.domain.auth.implementation

import java.util.UUID

import cats.implicits._
import com.google.inject.Inject
import javax.inject.Singleton

import tech.pegb.backoffice.dao.auth.abstraction.{BackOfficeUserDao, BusinessUnitDao}
import tech.pegb.backoffice.domain.auth._
import tech.pegb.backoffice.domain.auth.dto._
import tech.pegb.backoffice.domain.auth.model.BusinessUnit
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.auth.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.auth.Implicits._
import tech.pegb.backoffice.util.Constants.UnitInstance
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{UUIDLike, WithExecutionContexts}

import scala.concurrent.Future

@Singleton
class BusinessUnitService @Inject() (
    executionContexts: WithExecutionContexts,
    businessUnitDao: BusinessUnitDao,
    backOfficeUserDao: BackOfficeUserDao) extends abstraction.BusinessUnitService {

  implicit val ec = executionContexts.blockingIoOperations

  def create(dto: BusinessUnitToCreate, reactivateIfExisting: Boolean): Future[ServiceResponse[BusinessUnit]] = Future {
    val existingNameCriteria = BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = true)
    val existingNameButInactiveCriteria = BusinessUnitCriteria(name = Some(dto.name)).asDao(isActive = false)
    for {
      validatedCreateDto ← (BusinessUnit.isValidBusinessUnitName(dto.name), BusinessUnit.isValidCreatedBy(dto.createdBy)) match {
        case (false, _) ⇒
          validationError("Create business_unit failed. Name cannot be empty and cannot be longer than 32 characters.").toLeft
        case (_, false) ⇒
          validationError("Create business_unit failed. Created_by cannot be empty.").toLeft
        case _ ⇒ dto.toRight
      }

      //as per PR discussion, we should not rely in db constraints for business rules
      _ ← businessUnitDao.getBusinessUnitsByCriteria(existingNameCriteria, None, None, None)
        .map(_.headOption match {
          case Some(foundExisting) ⇒ duplicateError("Create business_unit failed. Same name already exists.").toLeft
          case _ ⇒ Right(None)
        }).leftMap(_.asDomainError)

      existingButInactive ← businessUnitDao.getBusinessUnitsByCriteria(existingNameButInactiveCriteria, None, None, None)
        .map(_.headOption.map(_.asDomain.get)).leftMap(_.asDomainError)

      updateOrCreateResult ← if (existingButInactive.isDefined && reactivateIfExisting) {
        val updateDto = BusinessUnitToUpdate(
          name = Some(validatedCreateDto.name),
          updatedBy = validatedCreateDto.createdBy,
          updatedAt = validatedCreateDto.createdAt,
          lastUpdatedAt = None).asDao(isActive = true)

        businessUnitDao.update(existingButInactive.get.id.toString, updateDto)
          .map(_.headOption.map(_.asDomain.get)).leftMap(_.asDomainError).fold(
            _.toLeft,
            {
              case Some(bu) ⇒ bu.toRight
              case None ⇒ notFoundError("Create business_unit failed. Inactive business_unit was not found.").toLeft
            })

      } else if (existingButInactive.isDefined && !reactivateIfExisting) {
        duplicateError("Create business_unit failed. Recreate flag must be set to true.").toLeft
      } else {
        businessUnitDao.create(validatedCreateDto.asDao).map(_.asDomain.get).leftMap(_.asDomainError)
      }

    } yield {
      updateOrCreateResult
    }
  }.recover {
    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def getAllActiveBusinessUnits(
    criteria: BusinessUnitCriteria,
    orderBy: Seq[Ordering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Future[ServiceResponse[Seq[BusinessUnit]]] = Future {

    businessUnitDao.getBusinessUnitsByCriteria(
      criteria.asDao(isActive = true),
      orderBy.asDao, maybeLimit, maybeOffset)
      .bimap(_.asDomainError, _.map(_.asDomain.get))

  }.recover {
    case error: IllegalArgumentException if error.getMessage.toLowerCase.contains("invalid uuid") ⇒
      logger.error("Error in getAllActiveBusinessUnits. Id from business_units table is not UUID", error)
      dtoMappingError("Unable to read business unit resource correctly. Id was not UUID").toLeft

    case error: Exception if error.getCause.isInstanceOf[AssertionError] ⇒
      logger.error("Error in getAllActiveBusinessUnits. Name or createdBy cannot be empty.", error)
      dtoMappingError("Unable to read business unit resource correctly. Name or created_by cannot be empty.").toLeft

    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def countAllActiveBusinessUnits(criteria: BusinessUnitCriteria): Future[ServiceResponse[Int]] = Future {
    businessUnitDao.countBusinessUnitsByCriteria(criteria.asDao(isActive = true))
      .bimap(_.asDomainError, identity)
  }.recover {
    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def update(id: UUID, dto: BusinessUnitToUpdate): Future[ServiceResponse[BusinessUnit]] = Future {
    (dto.name, dto.updatedBy, dto.updatedAt, dto.lastUpdatedAt) match {
      case (Some(name), _, _, _) if !BusinessUnit.isValidBusinessUnitName(name) ⇒
        validationError("Update business_unit failed. Name cannot be empty and cannot be longer than 32 characters.").toLeft
      case (_, updatedBy, _, _) if !BusinessUnit.isValidUpdatedBy(updatedBy.toOption) ⇒
        validationError("Update business_unit failed. Updated_by cannot be empty.").toLeft
      case (_, _, updatedAt, Some(lastUpdatedAt)) if updatedAt.isBefore(lastUpdatedAt) ⇒
        validationError("Update business_unit failed. Updated_at cannot be before last_updated_at.").toLeft

      case _ ⇒

        for {
          //added for front-end backwards compatibility
          maybeMissingLastUpdatedAt ← businessUnitDao.getBusinessUnitsByCriteria(
            BusinessUnitCriteria(id = id.some).asDao(isActive = true), None, None, None)
            .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError)

          result ← businessUnitDao.update(id.toString, dto.asDao(isActive = true, maybeMissingLastUpdatedAt))
            .fold(
              _.asDomainError.toLeft,
              {
                case Some(bu) ⇒ bu.asDomain.get.toRight
                case None ⇒ notFoundError(s"Update business_unit failed. Id [$id] not found.").toLeft
              })

        } yield {
          result
        }

    }

  }.recover {
    case error: IllegalArgumentException if error.getMessage.toLowerCase.contains("invalid uuid") ⇒
      val msg = "Update may have succeeded but unable to read business unit resource correctly. Id was not UUID"
      logger.error(msg, error)
      dtoMappingError(msg).toLeft

    case error: Exception if error.getCause.isInstanceOf[AssertionError] ⇒
      val msg = "Update may have succeeded but unable to read business unit resource correctly. Name or created_by cannot be empty."
      logger.error(msg, error)
      dtoMappingError(msg).toLeft

    case error: Throwable ⇒
      logger.error("Unexpected error", error)
      unknownError(s"Unexpected error. Please see the logs for more info.").toLeft
  }

  def remove(id: UUID, dto: BusinessUnitToRemove): Future[ServiceResponse[Unit]] = Future {
    val backOfficeUserCriteria = BackOfficeUserCriteria(businessUnitId = Some(UUIDLike(id.toString))).asDao(isActive = Some(true))
    for {
      validDto ← dto.removedBy.hasSomething
        .toEither(validationError("Remove business_unit failed. User cannot be empty."), dto)

      count ← backOfficeUserDao.countBackOfficeUsersByCriteria(Some(backOfficeUserCriteria))
        .fold(
          _.asDomainError.toLeft,
          result ⇒ (result == 0).toEither(validationError("Remove business_unit failed. One or more active back_office_users still belong to this business_unit."), 0))

      //added for front-end backwards compatibility
      maybeMissingLastUpdatedAt ← businessUnitDao.getBusinessUnitsByCriteria(
        BusinessUnitCriteria(id = id.some).asDao(isActive = true), None, None, None)
        .map(_.headOption.flatMap(_.updatedAt)).leftMap(_.asDomainError)

      result ← {
        val updateDto = BusinessUnitToUpdate.empty.copy(updatedAt = validDto.removedAt, updatedBy = validDto.removedBy,
          lastUpdatedAt = dto.lastUpdatedAt).asDao(isActive = false, maybeMissingLastUpdatedAt)
        businessUnitDao.update(id.toString, updateDto)
          .fold(
            _.asDomainError.toLeft,
            _ ⇒ Right(UnitInstance))
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
