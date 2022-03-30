package tech.pegb.backoffice.api.makerchecker.dto

trait WithMandatoryReason {
  val reason: String
}

case class RejectTaskRequest(reason: String) extends WithMandatoryReason
