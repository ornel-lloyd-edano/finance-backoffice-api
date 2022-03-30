package tech.pegb.backoffice.domain.limit.model

import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

sealed trait LimitType {
  def underlying: String

  override def toString: String = underlying
}

object LimitType {

  def apply(underlying: String): LimitType = {
    assert(underlying.hasSomething, "empty limit type")
    val tryLimitType = Try(fromString(underlying))
    assert(tryLimitType.isSuccess, s"invalid limit type: [$underlying]")
    tryLimitType.get
  }
  case object TransactionBased extends LimitType {
    override val underlying: String = "transaction_based"
  }
  case object BalanceBased extends LimitType {
    override val underlying: String = "balance_based"
  }

  def isBalanceBased(limitType: LimitType): Boolean = limitType == BalanceBased

  def fromString: PartialFunction[String, LimitType] = {
    case TransactionBased.`underlying` ⇒ TransactionBased
    case BalanceBased.`underlying` ⇒ BalanceBased
  }
}
