package tech.pegb.backoffice.api.swagger.model

case class SpreadsPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.currencyexchange.dto.SpreadToRead])
