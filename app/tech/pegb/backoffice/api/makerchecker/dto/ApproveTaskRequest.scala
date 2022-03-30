package tech.pegb.backoffice.api.makerchecker.dto

trait WithOptionalReason {
  val maybeReason: Option[String]
}

case class ApproveTaskRequest(maybeReason: Option[String]) extends WithOptionalReason
