package tech.pegb.backoffice.domain.customer.model

import tech.pegb.backoffice.util.Implicits._

case class UserType(underlying: String) {
  assert(underlying.hasSomething, "empty user type")
  assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""), s"invalid UserType: ${underlying}")
}

