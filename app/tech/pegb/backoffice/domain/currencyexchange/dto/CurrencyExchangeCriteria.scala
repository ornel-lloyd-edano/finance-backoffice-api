package tech.pegb.backoffice.domain.currencyexchange.dto

import java.util.Currency

import tech.pegb.backoffice.domain.currencyexchange.model.CurrencyExchangeStatus
import tech.pegb.backoffice.util.{HasPartialMatch, UUIDLike}

case class CurrencyExchangeCriteria(
    id: Option[UUIDLike] = None,
    dbId: Option[Int] = None,
    currencyCode: Option[Currency] = None,
    baseCurrency: Option[Currency] = None,
    provider: Option[String] = None,
    status: Option[CurrencyExchangeStatus] = None,
    partialMatchFields: Set[String] = Set.empty) extends HasPartialMatch
