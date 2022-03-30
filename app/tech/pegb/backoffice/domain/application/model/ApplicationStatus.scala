package tech.pegb.backoffice.domain.application.model

case class ApplicationStatus(underlying: String) {
  //assert(underlying.hasSomething, "empty ApplicationStatus")
  //assert(underlying.trim.toUpperCase.matches("""IN_PROCESS|PENDING|APPROVED|REJECTED"""), s"invalid ApplicationStatus [$underlying]")
}
