package tech.pegb.backoffice.domain

import java.util.UUID
import tech.pegb.backoffice.util.Logging
//TODO we already have a ServiceError factory in ServiceError companion object, do we really nead it in BaseService too?
//In my opinion, editing BaseService just because we have a new ServiceError is violation of Open-Closed principle
//so must need to remove this dependency from BaseService or we can have another factory here for ServiceError but more abstracted
//example:
//def getServiceError(errorCode: ErrorCode, message: String, maybeErrorRefId: Option[UUID] = None): ServiceError
trait BaseService extends Logging {
  type ServiceResponse[T] = Either[ServiceError, T]

  protected def unknownError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.unknownError(msg, maybeErrorRefId)
  }

  protected def partialSuccessError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.partialSuccessError(msg, maybeErrorRefId)
  }

  protected def duplicateError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.duplicateError(msg, maybeErrorRefId)
  }

  protected def notFoundError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.notFoundError(msg, maybeErrorRefId)
  }

  protected def validationError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    val refId = maybeErrorRefId.orElse(Some(UUID.randomUUID()))
    logger.error(s"[Validation Error][$refId] $msg")
    ServiceError.validationError(msg, refId)
  }

  protected def notAuthorizedError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.notAuthorizedError(msg, maybeErrorRefId)
  }

  protected def dtoMappingError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.dtoMappingError(msg, maybeErrorRefId)
  }

  protected def serviceUnimplementedError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.serviceUnimplementedError(msg, maybeErrorRefId)
  }

  protected def preconditionFailedError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.staleResourceAccessError(msg, maybeErrorRefId)
  }

  protected def timeOutError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError = {
    ServiceError.timeOutError(msg, maybeErrorRefId)
  }
}

object BaseService {

  type ServiceResponse[T] = Either[ServiceError, T]
  type BatchValidatedServiceResponse[T] = Either[Iterable[ServiceError], T]

}
