package tech.pegb.backoffice.api

import java.util.UUID

import scala.util.Try

final case class ApiError(id: UUID, code: ApiErrorCode, msg: String, trackingId: Option[UUID] = None) {
  def toMap = Map("id" → id.toString, "code" → code.toString, "msg" → msg, "tracking_id" → trackingId) //temporary workaround for jackson

  override def equals(obj: scala.Any): Boolean = {
    Try(obj.asInstanceOf[ApiError]).fold(_ ⇒ false, otherApiError ⇒ {
      this.code == otherApiError.code && this.msg == otherApiError.msg
    })
  }

}

object ApiErrors {
  def apply(code: ApiErrorCode, msg: String, trackingId: Option[UUID] = None)(implicit requestId: UUID): ApiError = {
    ApiError(requestId, code, msg, trackingId)
  }

  implicit class ThrowableToApiErrorAdapter(val arg: Throwable) extends AnyVal {
    def asApiError(customErrorMsg: Option[String] = None)(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.Unknown, customErrorMsg.getOrElse(arg.getMessage), None)
    }

    def asInvalidRequestApiError(customErrorMsg: Option[String] = None)(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.InvalidRequest, customErrorMsg.getOrElse(arg.getMessage), None)
    }

    def asMalformedRequestApiError(customErrorMsg: Option[String] = None)(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.MalformedRequest, customErrorMsg.getOrElse(arg.getMessage), None)
    }

    def asForbiddenApiError(customErrorMsg: Option[String] = None)(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.Forbidden, customErrorMsg.getOrElse(arg.getMessage), None)
    }
  }

}

sealed trait ApiErrorCode

object ApiErrorCodes {

  case object Unknown extends ApiErrorCode {
    override def toString = "Unknown"
  }

  case object Conflict extends ApiErrorCode {
    override def toString = "Conflict"
  }

  case object NotFound extends ApiErrorCode {
    override def toString = "NotFound"
  }

  case object InvalidRequest extends ApiErrorCode {
    override def toString = "InvalidRequest"
  }

  case object NotAuthorized extends ApiErrorCode {
    override def toString = "NotAuthorized"
  }

  case object Forbidden extends ApiErrorCode {
    override def toString = "PermissionsInsufficient"
  }

  case object MalformedRequest extends ApiErrorCode {
    override def toString = "MalformedRequest"
  }

  case object NotAvailable extends ApiErrorCode {
    override def toString = "NotAvailable"
  }

  case object PreconditionFailed extends ApiErrorCode {
    override def toString = "PreconditionFailed"
  }

  case object AccountTemporarilyLocked extends ApiErrorCode {
    override def toString = "AccountTemporarilyLocked"
  }

  case object CaptchaRequired extends ApiErrorCode {
    override def toString = "CaptchaRequired"
  }

  case object Timeout extends ApiErrorCode {
    override def toString = "Timeout"
  }

  def fromString(arg: String): ApiErrorCode = arg match {
    case "Unknown" ⇒ Unknown
    case "Conflict" ⇒ Conflict
    case "NotFound" ⇒ NotFound
    case "InvalidRequest" ⇒ InvalidRequest
    case "NotAuthorized" ⇒ NotAuthorized
    case "Forbidden" ⇒ Forbidden
    case "MalformedRequest" ⇒ MalformedRequest
    case "NotAvailable" ⇒ NotAvailable
    case "PreconditionFailed" ⇒ PreconditionFailed
    case "AccountTemporarilyLocked" ⇒ AccountTemporarilyLocked
    case "CaptchaRequired" ⇒ CaptchaRequired
    case "Timeout" ⇒ Timeout
  }
}
