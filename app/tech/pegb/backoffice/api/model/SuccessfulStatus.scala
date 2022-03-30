package tech.pegb.backoffice.api.model

sealed trait SuccessfulStatus

object SuccessfulStatuses {
  case object Ok extends SuccessfulStatus
  case object Accepted extends SuccessfulStatus
  case object Created extends SuccessfulStatus
  case object NoContent extends SuccessfulStatus
  case object Redirect extends SuccessfulStatus
}
