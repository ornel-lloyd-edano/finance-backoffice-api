package tech.pegb.backoffice.domain

import java.util.UUID

import scala.util.Try

trait ServiceError {
  def id: UUID

  def code: ErrorCode

  def message: String

  override def toString: String = s"$message ($id)"

  override def equals(obj: scala.Any): Boolean = {
    Try(obj.asInstanceOf[ServiceError].message == message).fold(_ ⇒ false, identity)
  }
}

object ServiceError {

  private def getOrGenerateUUIDThenExecute(id: Option[UUID], errorCode: ErrorCode, message: String, getServiceError: (UUID, ErrorCode, String) ⇒ ServiceError) = {
    getServiceError(id.getOrElse(UUID.randomUUID()), errorCode, message)
  }

  def unknownError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.Unknown, msg, UnknownError.apply)

  def partialSuccessError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.PartialSuccess, msg, PartialSuccess.apply)

  def duplicateError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.Duplicate, msg, DuplicateError.apply)

  def notFoundError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.NotFound, msg, NotFoundError.apply)

  def validationError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.ValidationFailed, msg, ValidationServiceError.apply)

  def notAuthorizedError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.ValidationFailed, msg, NotAuthorizedError.apply)

  def notAvailableError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.NotAvailable, msg, NotAvailableError.apply)

  def dtoMappingError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.BadFormat, msg, DTOMappingError.apply)

  def serviceUnimplementedError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.Unknown, msg, ServiceUnimplementedError.apply)

  def staleResourceAccessError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.StaleResourceAccess, msg, StaleResourceAccessError.apply)

  def externalServiceError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.Unknown, msg, ServiceUnimplementedError.apply)

  def insufficientPermissionsError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.PermissionsInsufficient, msg, ServiceUnimplementedError.apply)

  def accountLockedError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.AccountTemporarilyLocked, msg, ServiceUnimplementedError.apply)

  def captchaRequiredError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.CaptchaRequired, msg, ServiceUnimplementedError.apply)

  def timeOutError(msg: String, maybeErrorRefId: Option[UUID] = None): ServiceError =
    getOrGenerateUUIDThenExecute(maybeErrorRefId, ErrorCodes.TimeOut, msg, ServiceUnimplementedError.apply)

  private[domain] final case class UnknownError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class PartialSuccess(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class DuplicateError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class NotFoundError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class ValidationServiceError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class NotAuthorizedError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class DTOMappingError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class ServiceUnimplementedError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class StaleResourceAccessError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class NotAvailableError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

  private[domain] final case class TimeOutError(
      override val id: UUID,
      override val code: ErrorCode,
      override val message: String) extends ServiceError

}
