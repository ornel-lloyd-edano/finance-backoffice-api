package tech.pegb.backoffice.mapping.domain.dao.currencyexchange

import tech.pegb.backoffice.domain.currencyexchange.dto.{SpreadCriteria, SpreadToCreate, SpreadUpdateDto}
import tech.pegb.backoffice.dao.currencyexchange.dto.{SpreadToInsert, SpreadCriteria ⇒ DaoSpreadCritera}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.{dao, domain}

object Implicits {

  implicit class SpreadCriteriaAdapter(val arg: SpreadCriteria) extends AnyVal {
    def asDao = {
      DaoSpreadCritera(
        id = arg.id.map(id ⇒
          CriteriaField("id", id.underlying,
            if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
        currencyExchangeId = arg.currencyExchangeId.map(id ⇒
          CriteriaField("currency_exchange_id", id.underlying,
            if (arg.partialMatchFields.contains("currency_exchange_id")) MatchTypes.Partial else MatchTypes.Exact)),
        currencyCode = arg.currencyCode.map(_.getCurrencyCode),
        transactionType = arg.transactionType.map(_.underlying),
        channel = arg.channel.map(_.underlying),
        recipientInstitution = arg.recipientInstitution,
        isDeletedAtNotNull = arg.isDeleted)
    }
  }

  implicit class CurrencyExchangeCriteriaConverter(val arg: domain.currencyexchange.dto.CurrencyExchangeCriteria) extends AnyVal {
    def asDao = dao.currencyexchange.dto.CurrencyExchangeCriteria(
      id = arg.id.map(currencyId ⇒ CriteriaField("uuid", currencyId.underlying,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      dbId = arg.dbId.map(dbId ⇒ CriteriaField("id", dbId,
        if (arg.partialMatchFields.contains("dbId")) MatchTypes.Partial else MatchTypes.Exact)),
      currencyCode = arg.currencyCode.map(currencyCode ⇒ CriteriaField("currency_code", currencyCode.getCurrencyCode,
        if (arg.partialMatchFields.contains("currency_code")) MatchTypes.Partial else MatchTypes.Exact)),
      baseCurrency = arg.baseCurrency.map(baseCurrency ⇒ CriteriaField("base_currency", baseCurrency.getCurrencyCode,
        if (arg.partialMatchFields.contains("base_currency")) MatchTypes.Partial else MatchTypes.Exact)),
      provider = arg.provider.map(provider ⇒ CriteriaField("provider", provider,
        if (arg.partialMatchFields.contains("provider")) MatchTypes.Partial else MatchTypes.Exact)),
      status = arg.status.map(_.underlying))
  }

  implicit class SpreadsToCreateMapper(val arg: SpreadToCreate) extends AnyVal {
    def asDao = SpreadToInsert(
      currencyExchangeId = arg.currencyExchangeId,
      transactionType = arg.transactionType.underlying,
      channel = arg.channel.map(_.underlying),
      institution = arg.institution,
      spread = arg.spread,
      createdAt = arg.createdAt,
      createdBy = arg.createdBy)
  }

  implicit class SpreadUpdateDtoConverter(val arg: SpreadUpdateDto) extends AnyVal {
    def asDao = tech.pegb.backoffice.dao.currencyexchange.dto.SpreadUpdateDto(
      spread = arg.spread,
      updatedAt = arg.updatedAt,
      updatedBy = arg.updatedBy,
      deletedAt = None,
      lastUpdatedAt = arg.lastUpdatedAt)
  }
}
