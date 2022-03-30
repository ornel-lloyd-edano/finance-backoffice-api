package tech.pegb.backoffice.mapping.dao.domain.currencyexchange

import java.util.{Currency, UUID}

import tech.pegb.backoffice.dao
import tech.pegb.backoffice.dao.currencyexchange.entity.Spread
import tech.pegb.backoffice.domain.currencyexchange.model.{CurrencyExchange, Spread ⇒ DomainSpread}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}

import scala.util.Try

object Implicits {

  implicit class SpreadEntityAdapter(val arg: Spread) extends AnyVal {
    def asDomain(currencyExchange: CurrencyExchange) = {
      DomainSpread(
        id = arg.uuid,
        currencyExchange = currencyExchange, //To be filled in domain
        transactionType = TransactionType(arg.transactionType),
        channel = arg.channel.map(Channel),
        recipientInstitution = arg.recipientInstitution,
        spread = arg.spread,
        isDeleted = arg.deletedAt.isDefined,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt)
    }
  }

  implicit class CurrencyExchangeAdapter(arg: dao.currencyexchange.entity.CurrencyExchange) {
    def asDomain: Try[CurrencyExchange] = Try {
      CurrencyExchange(
        id = UUID.fromString(arg.uuid),
        currency = Currency.getInstance(arg.currencyCode),
        baseCurrency = Currency.getInstance(arg.baseCurrency),
        rate = arg.rate,
        provider = arg.provider,
        balance = arg.balance,
        dailyAmount = None, //To be filled up only in get CurrencyExchangeById
        status = arg.status,
        lastUpdated = arg.updatedAt)
    }
  }

}
