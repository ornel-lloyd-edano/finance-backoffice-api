package tech.pegb.backoffice.domain.currencyexchange.model

import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

trait CurrencyExchangeStatus {
  val underlying: String
}

object CurrencyExchangeStatus {
  def apply(underlying: String) = {
    assert(underlying.hasSomething, "empty currency exchange status")
    val tryStatus = Try(fromString(underlying))
    assert(tryStatus.isSuccess, s"invalid currency exchange status [$underlying]")
    tryStatus.get
  }

  case object Active extends CurrencyExchangeStatus {
    override val underlying: String = "active"
  }
  case object Inactive extends CurrencyExchangeStatus {
    override val underlying: String = "inactive"
  }

  def fromString: PartialFunction[String, CurrencyExchangeStatus] = {
    case Active.`underlying` ⇒ Active
    case Inactive.`underlying` ⇒ Inactive
  }
}
