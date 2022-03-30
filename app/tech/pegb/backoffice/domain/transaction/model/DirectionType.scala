package tech.pegb.backoffice.domain.transaction.model

import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

sealed trait DirectionType {
  val underlying: String
  override def toString() = underlying

  def isCredit: Boolean
  def isDebit: Boolean
}
object DirectionTypes {

  def apply(underlying: String): DirectionType = {
    assert(underlying.hasSomething, "empty direction")
    val tryDirection = Try(underlying.toTxnDirection)
    assert(tryDirection.isSuccess, s"invalid transaction direction [$underlying]")
    tryDirection.get
  }

  case object Credit extends DirectionType {
    override val underlying = "credit"
    override def isCredit = true
    override def isDebit = false
  }

  case object Debit extends DirectionType {
    override val underlying = "debit"
    override def isCredit = false
    override def isDebit = true
  }

  implicit class StringToTxnDirection(val arg: String) extends AnyVal {
    def toTxnDirection = arg.trim.toLowerCase match {
      case "credit" ⇒ Credit
      case "debit" ⇒ Debit
    }

    def toOppositeTxnDirection = arg.trim.toLowerCase match {
      case "credit" ⇒ Debit
      case "debit" ⇒ Credit
    }
  }

}
