package tech.pegb.backoffice.domain.transaction.model

import tech.pegb.backoffice.util.Implicits._

case class TransactionStatus(underlying: String) {
  assert(underlying.hasSomething, "empty TransactionStatus")
  //remove validation as per request of front end
  //assert(underlying.toLowerCase().matches("""pending|success|error|cancelled|reversed"""), s"invalid TransactionStatus [$underlying]")
}
