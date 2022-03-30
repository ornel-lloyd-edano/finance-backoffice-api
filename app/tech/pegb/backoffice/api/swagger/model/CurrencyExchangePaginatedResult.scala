package tech.pegb.backoffice.api.swagger.model

case class CurrencyExchangePaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.currencyexchange.dto.CurrencyExchangeToRead])
