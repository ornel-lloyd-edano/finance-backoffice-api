package tech.pegb.backoffice.mapping.api.domain.currencyexchange

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.{Currency, UUID}

import tech.pegb.backoffice.api.currencyexchange.dto.{SpreadToCreateWithFxId, SpreadToUpdate, SpreadToCreate ⇒ ApiSpreadToCreate}
import tech.pegb.backoffice.domain.currencyexchange.dto.{CurrencyExchangeCriteria, SpreadCriteria, SpreadToCreate, SpreadUpdateDto}
import tech.pegb.backoffice.domain.transaction.model.{Channel, TransactionType}
import tech.pegb.backoffice.domain.currencyexchange.model.CurrencyExchangeStatus
import tech.pegb.backoffice.util.UUIDLike
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  private type Id = Option[UUIDLike]
  private type CurrencyCode = Option[String]
  private type BaseCurrencyCode = Option[String]
  private type Provider = Option[String]
  private type Status = Option[String]
  private type PartialMatch = Set[String]
  implicit class CurrencyExchangeQueryParamsCriteriaAdapter(val arg: (Id, CurrencyCode, BaseCurrencyCode, Provider, Status, PartialMatch)) extends AnyVal {
    def asDomain = Try(
      CurrencyExchangeCriteria(
        id = arg._1,
        currencyCode = arg._2.map(Currency.getInstance),
        baseCurrency = arg._3.map(Currency.getInstance),
        provider = arg._4.map(_.sanitize),
        status = arg._5.map(CurrencyExchangeStatus(_)),
        partialMatchFields = arg._6))
  }

  private type CurrencyExchangeId = UUID
  private type SpreadsIdLike = Option[UUIDLike]
  private type CurrencyExchangeIdLike = Option[UUIDLike]
  private type TransactionType = Option[String]
  private type Channel = Option[String]
  private type Institution = Option[String]
  implicit class CurrencyExSpreadsQueryParamsCriteriaAdapter(val arg: (CurrencyExchangeId, TransactionType, Channel, Institution)) extends AnyVal {
    def asDomain = Try(SpreadCriteria(
      currencyExchangeId = arg._1.toOption.map(id ⇒ UUIDLike(id.toString)),
      transactionType = arg._2.map(TransactionType),
      channel = arg._3.map(Channel),
      recipientInstitution = arg._4))
  }
  implicit class SpreadsQueryParamsCriteriaAdapter(val arg: (SpreadsIdLike, CurrencyExchangeIdLike, CurrencyCode, TransactionType, Channel, Institution, PartialMatch)) extends AnyVal {
    def asDomain = Try(SpreadCriteria(
      id = arg._1,
      currencyExchangeId = arg._2.map(id ⇒ UUIDLike(id.toString)),
      currencyCode = arg._3.map(Currency.getInstance(_)),
      transactionType = arg._4.map(TransactionType),
      channel = arg._5.map(Channel),
      recipientInstitution = arg._6,
      partialMatchFields = arg._7))
  }

  implicit class SpreadToCreateAdapter(val arg: ApiSpreadToCreate) extends AnyVal {
    def asDomain(currencyExchange: UUID, createdBy: String, createdAt: LocalDateTime): Try[SpreadToCreate] =
      Try(SpreadToCreate(
        currencyExchangeId = currencyExchange,
        transactionType = TransactionType(arg.transactionType),
        channel = arg.channel.map(Channel),
        institution = arg.institution,
        spread = arg.spread,
        createdBy = createdBy,
        createdAt = createdAt))
  }

  implicit class SpreadToCreateWithFxIdAdapter(val arg: SpreadToCreateWithFxId) extends AnyVal {
    def asDomain(createdBy: String, createdAt: ZonedDateTime): Try[SpreadToCreate] =
      Try(SpreadToCreate(
        currencyExchangeId = arg.currencyExchangeId,
        transactionType = TransactionType(arg.transactionType),
        channel = arg.channel.map(Channel),
        institution = arg.institution,
        spread = arg.spread,
        createdBy = createdBy,
        createdAt = createdAt.toLocalDateTimeUTC))
  }

  implicit class SpreadUpdateDtoAdapter(val arg: SpreadToUpdate) extends AnyVal {
    def asDomain(spreadId: UUID, currencyExchangeId: UUID, updatedBy: String, updatedAt: ZonedDateTime): Try[SpreadUpdateDto] =
      Try(SpreadUpdateDto(
        id = spreadId,
        currencyExchangeId = currencyExchangeId,
        spread = arg.spread,
        updatedBy = updatedBy,
        updatedAt = updatedAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt))
  }

}
