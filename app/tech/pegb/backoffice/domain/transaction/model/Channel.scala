package tech.pegb.backoffice.domain.transaction.model
import tech.pegb.backoffice.util.Implicits._

//TODO modify Transaction model to use this, currently is used on Spread model
case class Channel(underlying: String) {
  assert(underlying.hasSomething, "empty channel")
  assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid channel: ${underlying}")
}
