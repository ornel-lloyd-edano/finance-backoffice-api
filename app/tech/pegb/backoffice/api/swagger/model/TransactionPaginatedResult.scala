package tech.pegb.backoffice.api.swagger.model

case class TransactionPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.transaction.dto.TransactionToRead])
