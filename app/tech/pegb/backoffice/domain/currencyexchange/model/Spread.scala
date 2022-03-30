package tech.pegb.backoffice.domain.currencyexchange.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.util.Implicits._
import Spread._
case class Spread(
    id: UUID,
    currencyExchange: CurrencyExchange,
    transactionType: TransactionType,
    channel: Option[Channel],
    recipientInstitution: Option[String],
    spread: BigDecimal,
    isDeleted: Boolean,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

  validate(transactionType, channel, recipientInstitution, spread)

}

object Spread {
  lazy val empty = new Spread(id = UUID.randomUUID(), currencyExchange = CurrencyExchange.empty,
    transactionType = TransactionType("currency_exchange"), channel = None, recipientInstitution = None,
    spread = BigDecimal(0), isDeleted = false,
    updatedAt = None, updatedBy = None)

  val minSpreadValue = BigDecimal(0)
  val maxSpreadValue = BigDecimal(1)

  def validate(
    transactionType: TransactionType,
    channel: Option[Channel],
    institution: Option[String],
    spread: BigDecimal): Unit = {
    assert(transactionType.underlying === "currency_exchange" || transactionType.underlying === "international_remittance", s"transaction type of [${transactionType.underlying}] is not allowed")
    if (transactionType.underlying === "currency_exchange") {
      assert(channel.isEmpty && institution.isEmpty, "channel or recipient institution cannot have value if transaction type is currency_exchange")
    }

    validateSpreadValue(spread)
  }

  def validateSpreadValue(spread: BigDecimal): Unit = {

    assert(spread >= minSpreadValue && spread <= maxSpreadValue, "spread must be from 0 to 1")
    assert(spread.scale <= 6, "spread value right significant digits must not be over 6")
  }
}
