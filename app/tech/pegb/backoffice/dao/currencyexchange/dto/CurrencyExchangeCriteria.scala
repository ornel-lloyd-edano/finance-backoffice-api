package tech.pegb.backoffice.dao.currencyexchange.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class CurrencyExchangeCriteria(
    id: Option[CriteriaField[String]] = None,
    dbId: Option[CriteriaField[Int]] = None,
    currencyCode: Option[CriteriaField[String]] = None,
    baseCurrency: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None,
    status: Option[String] = None)
