package tech.pegb.backoffice.domain

sealed trait ErrorCode

object ErrorCodes {
  case object Unknown extends ErrorCode
  case object PartialSuccess extends ErrorCode
  case object Duplicate extends ErrorCode
  case object NotFound extends ErrorCode
  case object ValidationFailed extends ErrorCode
  case object NotAuthorized extends ErrorCode
  case object PermissionsInsufficient extends ErrorCode
  case object BadFormat extends ErrorCode
  case object StaleResourceAccess extends ErrorCode
  case object ExternalServiceError extends ErrorCode
  case object NotAvailable extends ErrorCode
  case object AccountTemporarilyLocked extends ErrorCode
  case object CaptchaRequired extends ErrorCode
  case object TimeOut extends ErrorCode
}

