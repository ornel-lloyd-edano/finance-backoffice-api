package tech.pegb.backoffice.mapping.domain.api

import java.util.UUID

import tech.pegb.backoffice.api.{ApiError, ApiErrorCode, ApiErrorCodes}
import tech.pegb.backoffice.domain.{ErrorCode, ErrorCodes, ServiceError}

object Implicits {

  implicit class ServiceErrorCodeApiAdapter(val arg: ErrorCode) extends AnyVal {
    def asApiError: ApiErrorCode = arg match {
      case ErrorCodes.Duplicate ⇒ ApiErrorCodes.Conflict
      case ErrorCodes.NotFound ⇒ ApiErrorCodes.NotFound
      case ErrorCodes.ValidationFailed ⇒ ApiErrorCodes.InvalidRequest
      case ErrorCodes.NotAuthorized ⇒ ApiErrorCodes.NotAuthorized
      case ErrorCodes.PermissionsInsufficient ⇒ ApiErrorCodes.Forbidden
      case ErrorCodes.BadFormat ⇒ ApiErrorCodes.MalformedRequest
      case ErrorCodes.StaleResourceAccess ⇒ ApiErrorCodes.PreconditionFailed
      case ErrorCodes.ExternalServiceError ⇒ ApiErrorCodes.NotAvailable
      case ErrorCodes.AccountTemporarilyLocked ⇒ ApiErrorCodes.AccountTemporarilyLocked
      case ErrorCodes.CaptchaRequired ⇒ ApiErrorCodes.CaptchaRequired
      case ErrorCodes.TimeOut ⇒ ApiErrorCodes.Timeout
      case _ ⇒ ApiErrorCodes.Unknown
    }
  }

  implicit class ServiceErrorApiAdapter(val arg: ServiceError) extends AnyVal {
    def asApiError(customMessage: Option[String] = None)(implicit requestId: UUID) =
      ApiError(id = requestId, code = arg.code.asApiError, msg =
        customMessage.getOrElse(arg.message), trackingId = Some(arg.id))
  }

}
