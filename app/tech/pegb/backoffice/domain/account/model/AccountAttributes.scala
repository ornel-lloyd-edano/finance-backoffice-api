package tech.pegb.backoffice.domain.account.model

import tech.pegb.backoffice.util.Constants
import tech.pegb.backoffice.util.Implicits._

object AccountAttributes {

  case class AccountNumber(underlying: String) {
    assert(underlying.hasSomething, "empty AccountNumber")
    /*assert(underlying.matches("""[A-Za-z0-9\- ]+"""), "invalid AccountNumber")*/
    override def toString() = underlying
  }

  case class AccountStatus(underlying: String) {
    assert(underlying.hasSomething, "empty AccountStatus")
    /*assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""), s"invalid AccountStatus: ${underlying}")*/
  }

  case class AccountType(underlying: String) {
    assert(underlying.hasSomething, "empty AccountType")
    /* assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), "invalid AccountType")*/
  }

  case class AccountMainType(underlying: String) {
    assert(underlying.hasSomething, "empty AccountType")
    assert(underlying == Constants.Liability || underlying == Constants.Asset, "AccountMainType should only be liability or asset")
  }

}
