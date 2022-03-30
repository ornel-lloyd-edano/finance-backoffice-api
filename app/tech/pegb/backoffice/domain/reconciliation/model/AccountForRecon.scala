package tech.pegb.backoffice.domain.reconciliation.model

import java.time.LocalDateTime
import java.util.Currency
import tech.pegb.backoffice.util.Implicits._

case class AccountForRecon(
    id: String,
    accountName: String,
    accountNumber: String,
    userUuid: String,
    currency: Currency,
    `type`: String,
    mainType: AccountMainType,
    currentBalance: BigDecimal,
    previousDaySummaryEod: Option[BigDecimal] = None,
    lastTransactionDateTime: Option[LocalDateTime]) {
  def isLiability: Boolean = mainType == AccountMainTypes.Liability
}

object AccountForRecon {
  val empty = AccountForRecon(
    id = "",
    accountName = "",
    accountNumber = "",
    userUuid = "",
    currency = Currency.getInstance("KES"),
    `type` = "",
    mainType = AccountMainTypes.Liability,
    currentBalance = BigDecimal(0.0),
    previousDaySummaryEod = Some(BigDecimal(0.0)),
    lastTransactionDateTime = None)
}

trait AccountMainType {
  val underlying: String

  override def toString: String = underlying
}

object AccountMainTypes {

  case object Liability extends AccountMainType {
    override val underlying: String = "liability"
  }

  case object Asset extends AccountMainType {
    override val underlying: String = "asset"
  }

  case class UnknownAccountMainType(underlying: String) extends AccountMainType

  def fromString: PartialFunction[String, AccountMainType] = {
    case liability if liability === Liability.underlying ⇒ Liability
    case asset if asset === Asset.underlying ⇒ Asset
    case other ⇒ UnknownAccountMainType(other)
  }

}
